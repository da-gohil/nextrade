package com.nextrade.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrade.common.dto.event.OrderCancelledEvent;
import com.nextrade.common.dto.event.OrderCreatedEvent;
import com.nextrade.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryKafkaConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.events", groupId = "inventory-service")
    public void handleOrderEvent(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("eventType");
            log.info("Received order event: {}", eventType);

            if ("ORDER_CREATED".equals(eventType)) {
                OrderCreatedEvent event = objectMapper.convertValue(payload, OrderCreatedEvent.class);
                inventoryService.reserveStock(event);
            } else if ("ORDER_CANCELLED".equals(eventType)) {
                OrderCancelledEvent event = objectMapper.convertValue(payload, OrderCancelledEvent.class);
                inventoryService.releaseStock(event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }
}
