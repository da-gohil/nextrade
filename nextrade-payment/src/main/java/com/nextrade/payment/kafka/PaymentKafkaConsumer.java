package com.nextrade.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrade.common.dto.event.OrderCreatedEvent;
import com.nextrade.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.events", groupId = "payment-service")
    public void handleInventoryEvent(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("eventType");
            if ("INVENTORY_RESERVED".equals(eventType)) {
                // Inventory is reserved, now get the original order details
                // The payment service will be triggered by the order service via order.events
                log.info("Payment service received INVENTORY_RESERVED event");
            }
        } catch (Exception e) {
            log.error("Error in payment inventory consumer: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.events", groupId = "payment-service")
    public void handleOrderEvent(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("eventType");
            log.info("Payment service received order event: {}", eventType);

            if ("ORDER_CREATED".equals(eventType)) {
                OrderCreatedEvent event = objectMapper.convertValue(payload, OrderCreatedEvent.class);
                paymentService.handleOrderCreated(event);
            }
        } catch (Exception e) {
            log.error("Error in payment order consumer: {}", e.getMessage(), e);
        }
    }
}
