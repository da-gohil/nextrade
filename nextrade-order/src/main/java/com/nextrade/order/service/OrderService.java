package com.nextrade.order.service;

import com.nextrade.common.dto.PageResponse;
import com.nextrade.common.dto.event.*;
import com.nextrade.common.exception.BadRequestException;
import com.nextrade.common.exception.ResourceNotFoundException;
import com.nextrade.order.dto.*;
import com.nextrade.order.entity.*;
import com.nextrade.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicLong orderCounter = new AtomicLong(1);

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, Long userId) {
        String orderNumber = generateOrderNumber();

        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        List<OrderItem> items = request.getItems().stream().map(req -> OrderItem.builder()
                .order(order)
                .productId(req.getProductId())
                .productName(req.getProductName())
                .quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice())
                .subtotal(req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity())))
                .build()).toList();

        order.setItems(items);
        Order saved = orderRepository.save(order);

        addStatusHistory(saved, null, OrderStatus.PENDING, userId, "Order created");

        // Publish ORDER_CREATED event
        List<OrderCreatedEvent.OrderItem> eventItems = request.getItems().stream()
                .map(i -> new OrderCreatedEvent.OrderItem(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        kafkaTemplate.send("order.events", String.valueOf(saved.getId()),
                new OrderCreatedEvent(saved.getId(), orderNumber, userId, eventItems, total, request.getShippingAddress()));

        log.info("Order created: {} for user {}", orderNumber, userId);
        return mapToDto(saved);
    }

    public PageResponse<OrderDto> getOrders(Long userId, String role, String status, Pageable pageable) {
        Page<Order> page;
        boolean isAdmin = "ADMIN".equals(role) || "VENDOR".equals(role);

        if (status != null) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            page = isAdmin ? orderRepository.findByStatus(orderStatus, pageable)
                           : orderRepository.findByUserIdAndStatus(userId, orderStatus, pageable);
        } else {
            page = isAdmin ? orderRepository.findAll(pageable)
                           : orderRepository.findByUserId(userId, pageable);
        }

        return PageResponse.<OrderDto>builder()
                .content(page.getContent().stream().map(this::mapToDto).toList())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .build();
    }

    public OrderDto getOrder(Long id) {
        return mapToDto(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id)));
    }

    @Transactional
    public OrderDto updateStatus(Long id, UpdateOrderStatusRequest request, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        addStatusHistory(order, oldStatus, newStatus, userId, request.getNote());

        kafkaTemplate.send("order.events", String.valueOf(id),
                new OrderStatusUpdatedEvent(id, order.getOrderNumber(), oldStatus.name(), newStatus.name()));

        // Push real-time update via WebSocket
        messagingTemplate.convertAndSend("/topic/orders/" + id,
                Map.of("orderId", id, "status", newStatus.name(), "timestamp", LocalDateTime.now().toString()));

        return mapToDto(order);
    }

    @Transactional
    public OrderDto cancelOrder(Long id, Long userId, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel order in status: " + order.getStatus());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        addStatusHistory(order, oldStatus, OrderStatus.CANCELLED, userId, reason != null ? reason : "Cancelled by user");

        kafkaTemplate.send("order.events", String.valueOf(id),
                new OrderCancelledEvent(id, order.getOrderNumber(), reason));

        // Send notification
        kafkaTemplate.send("notification.events", String.valueOf(userId),
                new NotificationEvent(userId, "ORDER_CANCELLED",
                        "Order Cancelled",
                        "Your order " + order.getOrderNumber() + " has been cancelled.",
                        Map.of("orderId", String.valueOf(id), "orderNumber", order.getOrderNumber())));

        return mapToDto(order);
    }

    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            OrderStatus old = order.getStatus();
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);
            addStatusHistory(order, old, OrderStatus.PAYMENT_PROCESSING, null, "Inventory reserved, processing payment");
            log.info("Order {} moved to PAYMENT_PROCESSING", order.getOrderNumber());
        });
    }

    @Transactional
    public void handleInventoryFailed(InventoryReservationFailedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            OrderStatus old = order.getStatus();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            addStatusHistory(order, old, OrderStatus.CANCELLED, null, "Inventory reservation failed: " + event.getReason());
            log.info("Order {} CANCELLED due to inventory failure", order.getOrderNumber());
        });
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            OrderStatus old = order.getStatus();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            addStatusHistory(order, old, OrderStatus.PAID, null, "Payment completed");

            kafkaTemplate.send("notification.events", String.valueOf(order.getUserId()),
                    new NotificationEvent(order.getUserId(), "PAYMENT_SUCCESS",
                            "Payment Successful",
                            "Payment for order " + order.getOrderNumber() + " was successful!",
                            Map.of("orderId", String.valueOf(order.getId()), "amount", event.getAmount().toString())));

            messagingTemplate.convertAndSend("/topic/orders/" + order.getId(),
                    Map.of("orderId", order.getId(), "status", "PAID", "timestamp", LocalDateTime.now().toString()));

            log.info("Order {} marked as PAID", order.getOrderNumber());
        });
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            OrderStatus old = order.getStatus();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            addStatusHistory(order, old, OrderStatus.CANCELLED, null, "Payment failed: " + event.getReason());

            // Release inventory
            kafkaTemplate.send("order.events", String.valueOf(order.getId()),
                    new OrderCancelledEvent(order.getId(), order.getOrderNumber(), "Payment failed"));

            kafkaTemplate.send("notification.events", String.valueOf(order.getUserId()),
                    new NotificationEvent(order.getUserId(), "PAYMENT_FAILED",
                            "Payment Failed",
                            "Payment for order " + order.getOrderNumber() + " failed. Reason: " + event.getReason(),
                            Map.of("orderId", String.valueOf(order.getId()))));

            log.info("Order {} CANCELLED due to payment failure", order.getOrderNumber());
        });
    }

    private void addStatusHistory(Order order, OrderStatus from, OrderStatus to, Long changedBy, String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to.name())
                .changedBy(changedBy)
                .note(note)
                .build();
        order.getStatusHistory().add(history);
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("NXT-%s-%04d", date, orderCounter.getAndIncrement());
    }

    private OrderDto mapToDto(Order order) {
        List<OrderDto.OrderItemDto> items = order.getItems().stream().map(item ->
                OrderDto.OrderItemDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build()).toList();

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
