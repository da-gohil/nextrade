package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryReservedEvent extends BaseEvent {
    private Long orderId;
    private List<Reservation> reservations;

    public InventoryReservedEvent(Long orderId, List<Reservation> reservations) {
        this.setEventType("INVENTORY_RESERVED");
        this.setSource("inventory-service");
        this.orderId = orderId;
        this.reservations = reservations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reservation {
        private Long productId;
        private Integer quantity;
        private Long reservationId;
    }
}
