package com.nextrade.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String reason;
}
