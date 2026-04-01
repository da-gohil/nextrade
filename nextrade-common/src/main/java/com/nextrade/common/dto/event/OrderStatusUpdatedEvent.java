package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderStatusUpdatedEvent extends BaseEvent {
    private Long orderId;
    private String orderNumber;
    private String fromStatus;
    private String toStatus;

    public OrderStatusUpdatedEvent(Long orderId, String orderNumber, String fromStatus, String toStatus) {
        this.setEventType("ORDER_STATUS_UPDATED");
        this.setSource("order-service");
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
}
