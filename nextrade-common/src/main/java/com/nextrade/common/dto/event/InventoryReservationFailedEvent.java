package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryReservationFailedEvent extends BaseEvent {
    private Long orderId;
    private List<FailedItem> failedItems;
    private String reason;

    public InventoryReservationFailedEvent(Long orderId, List<FailedItem> failedItems, String reason) {
        this.setEventType("INVENTORY_RESERVATION_FAILED");
        this.setSource("inventory-service");
        this.orderId = orderId;
        this.failedItems = failedItems;
        this.reason = reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private Long productId;
        private Integer requestedQty;
        private Integer availableQty;
    }
}
