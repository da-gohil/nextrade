package com.nextrade.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty
    private List<OrderItemRequest> items;

    @NotBlank
    private String shippingAddress;

    private String notes;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private Long productId;

        @NotBlank
        private String productName;

        @NotNull @Min(1)
        private Integer quantity;

        @NotNull @Positive
        private BigDecimal unitPrice;
    }
}
