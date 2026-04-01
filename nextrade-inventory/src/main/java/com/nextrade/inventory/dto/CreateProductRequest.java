package com.nextrade.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotBlank
    private String sku;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Long categoryId;

    @NotNull @Positive
    private BigDecimal price;

    @Min(0)
    private Integer stockQuantity = 0;

    @Min(1)
    private Integer lowStockThreshold = 10;

    private String imageUrl;
}
