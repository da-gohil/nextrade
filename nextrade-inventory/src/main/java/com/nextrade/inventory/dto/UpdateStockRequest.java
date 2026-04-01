package com.nextrade.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStockRequest {
    @NotNull
    private Integer quantity; // can be negative (decrease) or positive (increase)
    private String reason;
}
