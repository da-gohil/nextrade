package com.nextrade.inventory.service;

import com.nextrade.common.dto.PageResponse;
import com.nextrade.common.dto.event.*;
import com.nextrade.common.exception.BadRequestException;
import com.nextrade.common.exception.ConflictException;
import com.nextrade.common.exception.ResourceNotFoundException;
import com.nextrade.inventory.dto.*;
import com.nextrade.inventory.entity.*;
import com.nextrade.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto createProduct(CreateProductRequest request, Long vendorId) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("Product with SKU already exists: " + request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .vendorId(vendorId)
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();

        product = productRepository.save(product);
        log.info("Product created: {} (SKU: {})", product.getName(), product.getSku());
        return mapToDto(product);
    }

    @Cacheable(value = "products", key = "#search + '_' + #categoryId + '_' + #pageable.pageNumber")
    public PageResponse<ProductDto> getProducts(String search, Long categoryId, Pageable pageable) {
        Page<Product> page = productRepository.findActiveProducts(search, categoryId, pageable);
        return PageResponse.<ProductDto>builder()
                .content(page.getContent().stream().map(this::mapToDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public ProductDto getProduct(Long id) {
        return mapToDto(productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id)));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto updateProduct(Long id, CreateProductRequest request, Long vendorId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setPrice(request.getPrice());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setImageUrl(request.getImageUrl());

        return mapToDto(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto adjustStock(Long id, UpdateStockRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        int newQty = product.getStockQuantity() + request.getQuantity();
        if (newQty < 0) {
            throw new BadRequestException("Insufficient stock. Current: " + product.getStockQuantity());
        }

        product.setStockQuantity(newQty);
        product = productRepository.save(product);

        // Check for low stock
        if (product.getAvailableQuantity() <= product.getLowStockThreshold()) {
            publishLowStockAlert(product);
        }

        return mapToDto(product);
    }

    @Transactional
    public void reserveStock(OrderCreatedEvent event) {
        Long orderId = event.getOrderId();
        List<InventoryReservedEvent.Reservation> reservations = new ArrayList<>();
        List<InventoryReservationFailedEvent.FailedItem> failedItems = new ArrayList<>();

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);

            if (product == null || !product.getIsActive()) {
                failedItems.add(new InventoryReservationFailedEvent.FailedItem(
                        item.getProductId(), item.getQuantity(), 0));
                continue;
            }

            int available = product.getAvailableQuantity();
            if (available < item.getQuantity()) {
                failedItems.add(new InventoryReservationFailedEvent.FailedItem(
                        item.getProductId(), item.getQuantity(), available));
            } else {
                product.setReservedQuantity(product.getReservedQuantity() + item.getQuantity());
                productRepository.save(product);

                StockReservation reservation = StockReservation.builder()
                        .product(product)
                        .orderId(orderId)
                        .quantity(item.getQuantity())
                        .status(StockReservation.Status.RESERVED)
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                        .build();
                reservation = reservationRepository.save(reservation);

                reservations.add(new InventoryReservedEvent.Reservation(
                        product.getId(), item.getQuantity(), reservation.getId()));
            }
        }

        if (!failedItems.isEmpty()) {
            // Rollback any successful reservations
            for (InventoryReservedEvent.Reservation r : reservations) {
                releaseReservation(r.getReservationId());
            }
            kafkaTemplate.send("inventory.events",
                    String.valueOf(orderId),
                    new InventoryReservationFailedEvent(orderId, failedItems, "Insufficient stock"));
            log.info("Inventory reservation FAILED for order {}", orderId);
        } else {
            kafkaTemplate.send("inventory.events",
                    String.valueOf(orderId),
                    new InventoryReservedEvent(orderId, reservations));
            log.info("Inventory reserved for order {}", orderId);
        }
    }

    @Transactional
    public void releaseStock(Long orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (StockReservation r : reservations) {
            if (r.getStatus() == StockReservation.Status.RESERVED) {
                releaseReservation(r.getId());
            }
        }
        log.info("Stock released for order {}", orderId);
    }

    private void releaseReservation(Long reservationId) {
        StockReservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation != null && reservation.getStatus() == StockReservation.Status.RESERVED) {
            Product product = reservation.getProduct();
            product.setReservedQuantity(Math.max(0, product.getReservedQuantity() - reservation.getQuantity()));
            productRepository.save(product);
            reservation.setStatus(StockReservation.Status.RELEASED);
            reservationRepository.save(reservation);
        }
    }

    @Scheduled(fixedDelay = 60000) // every minute
    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        for (StockReservation r : expired) {
            releaseReservation(r.getId());
            log.info("Auto-released expired reservation {} for order {}", r.getId(), r.getOrderId());
        }
    }

    private void publishLowStockAlert(Product product) {
        NotificationEvent notificationEvent = new NotificationEvent(
                null, "LOW_STOCK",
                "Low Stock Alert",
                "Product '" + product.getName() + "' (SKU: " + product.getSku() + ") is low on stock. Available: " + product.getAvailableQuantity(),
                Map.of("productId", String.valueOf(product.getId()), "sku", product.getSku())
        );
        kafkaTemplate.send("notification.events", String.valueOf(product.getId()), notificationEvent);
    }

    private ProductDto mapToDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .vendorId(p.getVendorId())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .reservedQuantity(p.getReservedQuantity())
                .availableQuantity(p.getAvailableQuantity())
                .lowStockThreshold(p.getLowStockThreshold())
                .imageUrl(p.getImageUrl())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
