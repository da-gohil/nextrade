package com.nextrade.payment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long id;
    private String paymentNumber;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String method;
    private String status;
    private String idempotencyKey;
    private String failureReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
