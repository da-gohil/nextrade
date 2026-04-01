package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends BaseEvent {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String shippingAddress;

    public OrderCreatedEvent(Long orderId, String orderNumber, Long userId,
                              List<OrderItem> items, BigDecimal totalAmount, String shippingAddress) {
        this.setEventType("ORDER_CREATED");
        this.setSource("order-service");
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
