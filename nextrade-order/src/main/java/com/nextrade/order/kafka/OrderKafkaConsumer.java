package com.nextrade.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrade.common.dto.event.*;
import com.nextrade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.events", groupId = "order-service")
    public void handleInventoryEvent(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("eventType");
            log.info("Received inventory event: {}", eventType);

            if ("INVENTORY_RESERVED".equals(eventType)) {
                InventoryReservedEvent event = objectMapper.convertValue(payload, InventoryReservedEvent.class);
                orderService.handleInventoryReserved(event);
            } else if ("INVENTORY_RESERVATION_FAILED".equals(eventType)) {
                InventoryReservationFailedEvent event = objectMapper.convertValue(payload, InventoryReservationFailedEvent.class);
                orderService.handleInventoryFailed(event);
            }
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.events", groupId = "order-service")
    public void handlePaymentEvent(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("eventType");
            log.info("Received payment event: {}", eventType);

            if ("PAYMENT_COMPLETED".equals(eventType)) {
                PaymentCompletedEvent event = objectMapper.convertValue(payload, PaymentCompletedEvent.class);
                orderService.handlePaymentCompleted(event);
            } else if ("PAYMENT_FAILED".equals(eventType)) {
                PaymentFailedEvent event = objectMapper.convertValue(payload, PaymentFailedEvent.class);
                orderService.handlePaymentFailed(event);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}
