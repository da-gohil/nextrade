package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends BaseEvent {
    private Long paymentId;
    private Long orderId;
    private String reason;

    public PaymentFailedEvent(Long paymentId, Long orderId, String reason) {
        this.setEventType("PAYMENT_FAILED");
        this.setSource("payment-service");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.reason = reason;
    }
}
