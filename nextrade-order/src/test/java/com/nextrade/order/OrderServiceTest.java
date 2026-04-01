package com.nextrade.order;

import com.nextrade.common.dto.event.InventoryReservedEvent;
import com.nextrade.common.dto.event.PaymentCompletedEvent;
import com.nextrade.order.entity.Order;
import com.nextrade.order.entity.OrderStatus;
import com.nextrade.order.repository.OrderRepository;
import com.nextrade.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("handleInventoryReserved should move order to PAYMENT_PROCESSING")
    void handleInventoryReserved_shouldUpdateStatus() {
        var order = Order.builder()
            .id(1L).orderNumber("NXT-20260401-0001")
            .userId(1L).status(OrderStatus.PENDING)
            .totalAmount(BigDecimal.valueOf(100)).shippingAddress("123 Main St")
            .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var event = new InventoryReservedEvent(1L, List.of());
        orderService.handleInventoryReserved(event);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_PROCESSING));
    }

    @Test
    @DisplayName("handlePaymentCompleted should move order to PAID")
    void handlePaymentCompleted_shouldUpdateStatus() {
        var order = Order.builder()
            .id(1L).orderNumber("NXT-20260401-0001")
            .userId(1L).status(OrderStatus.PAYMENT_PROCESSING)
            .totalAmount(BigDecimal.valueOf(100)).shippingAddress("123 Main St")
            .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var event = new PaymentCompletedEvent(1L, 1L, BigDecimal.valueOf(100), "CREDIT_CARD");
        orderService.handlePaymentCompleted(event);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAID));
        verify(kafkaTemplate).send(eq("notification.events"), any(), any());
    }
}
