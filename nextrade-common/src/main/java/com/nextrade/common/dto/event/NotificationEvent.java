package com.nextrade.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationEvent extends BaseEvent {
    private Long userId;
    private String type;
    private String title;
    private String message;
    private Map<String, String> notificationMetadata;

    public NotificationEvent(Long userId, String type, String title, String message, Map<String, String> notificationMetadata) {
        this.setEventType("NOTIFICATION_SEND");
        this.setSource("notification-service");
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.notificationMetadata = notificationMetadata;
    }
}
