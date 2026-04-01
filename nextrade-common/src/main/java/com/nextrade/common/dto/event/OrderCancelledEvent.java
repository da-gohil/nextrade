package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends BaseEvent {
    private Long orderId;
    private String orderNumber;
    private String reason;

    public OrderCancelledEvent(Long orderId, String orderNumber, String reason) {
        this.setEventType("ORDER_CANCELLED");
        this.setSource("order-service");
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.reason = reason;
    }
}
