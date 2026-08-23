package com.tiki.notification.service;

import com.tiki.notification.entity.NotificationEntity;
import com.tiki.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<NotificationEntity> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public List<NotificationEntity> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<NotificationEntity> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public NotificationEntity sendNotification(Long userId, String title, String message, String type, String targetUrl) {
        NotificationEntity notification = NotificationEntity.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .targetUrl(targetUrl)
                .build();
        log.info("Sending notification: {} to user: {}", title, userId);
        NotificationEntity saved = notificationRepository.save(notification);
        
        // Push to WebSocket
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, Map.of(
                "id", saved.getId(),
                "title", saved.getTitle(),
                "message", saved.getMessage(),
                "type", saved.getType(),
                "targetUrl", saved.getTargetUrl(),
                "createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : ""
            ));
            log.info("Notification pushed to WebSocket destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to push notification to WebSocket", e);
        }
        
        return saved;
    }

    public void broadcastFlashSaleUpdate(Object event) {
        try {
            messagingTemplate.convertAndSend("/topic/flash-sale", event);
            log.info("Flash sale update broadcasted via WebSocket to /topic/flash-sale");
        } catch (Exception e) {
            log.error("Failed to broadcast flash sale update", e);
        }
    }
}
