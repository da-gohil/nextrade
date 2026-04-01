package com.nextrade.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrade.common.dto.event.NotificationEvent;
import com.nextrade.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.events", groupId = "notification-service")
    public void handleNotificationEvent(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("eventType");
            log.info("Received notification event: {}", eventType);

            if ("NOTIFICATION_SEND".equals(eventType)) {
                NotificationEvent event = objectMapper.convertValue(payload, NotificationEvent.class);
                notificationService.handleNotificationEvent(event);
            }
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }
}
