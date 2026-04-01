package com.nextrade.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrade.common.dto.PageResponse;
import com.nextrade.common.dto.event.NotificationEvent;
import com.nextrade.common.exception.ResourceNotFoundException;
import com.nextrade.notification.dto.NotificationDto;
import com.nextrade.notification.entity.Notification;
import com.nextrade.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        if (event.getUserId() == null) {
            log.warn("Notification event has no userId, skipping persistence");
            return;
        }

        String metadataJson = null;
        if (event.getNotificationMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(event.getNotificationMetadata());
            } catch (Exception e) {
                log.warn("Could not serialize notification metadata");
            }
        }

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .title(event.getTitle())
                .message(event.getMessage())
                .isRead(false)
                .metadata(metadataJson)
                .build();

        notification = notificationRepository.save(notification);

        // Push real-time via WebSocket
        NotificationDto dto = mapToDto(notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + event.getUserId(), dto);

        log.info("Notification sent to user {}: [{}] {}", event.getUserId(), event.getType(), event.getTitle());
    }

    public PageResponse<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.<NotificationDto>builder()
                .content(page.getContent().stream().map(this::mapToDto).toList())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        return mapToDto(n);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId()).userId(n.getUserId())
                .type(n.getType()).title(n.getTitle())
                .message(n.getMessage()).isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
