package com.tiki.notification.service;

import com.tiki.notification.entity.NotificationEntity;
import com.tiki.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("getUserNotifications - returns paged notifications")
    void getUserNotifications() {
        Page<NotificationEntity> page = new PageImpl<>(List.of(new NotificationEntity()));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class))).thenReturn(page);

        Page<NotificationEntity> result = notificationService.getUserNotifications(1L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getUnreadNotifications - returns unread list")
    void getUnreadNotifications() {
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(new NotificationEntity()));

        List<NotificationEntity> result = notificationService.getUnreadNotifications(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("markAsRead - updates flag and saves")
    void markAsRead() {
        NotificationEntity notification = NotificationEntity.builder().id(100L).isRead(false).build();
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(100L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAllAsRead - updates flag for all unread")
    void markAllAsRead() {
        NotificationEntity n1 = NotificationEntity.builder().isRead(false).build();
        NotificationEntity n2 = NotificationEntity.builder().isRead(false).build();
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead(1L);

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository).saveAll(any());
    }

    @Test
    @DisplayName("sendNotification - saves and pushes to WebSocket")
    void sendNotification() {
        NotificationEntity saved = NotificationEntity.builder()
                .id(50L).title("Test").message("Msg").type("INFO").targetUrl("/home").build();
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(saved);

        NotificationEntity result = notificationService.sendNotification(1L, "Test", "Msg", "INFO", "/home");

        assertThat(result.getId()).isEqualTo(50L);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/1"), any(Object.class));
    }
}
