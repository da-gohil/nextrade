package com.nextrade.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProcessPaymentRequest {
    @NotNull
    private Long orderId;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String method;

    @NotBlank
    private String idempotencyKey;
}
