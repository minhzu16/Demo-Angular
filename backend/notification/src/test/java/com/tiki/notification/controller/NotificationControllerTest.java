package com.tiki.notification.controller;

import com.tiki.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController controller;

    private static HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    @DisplayName("Không đọc / đánh dấu đã đọc thông báo của người khác")
    void otherUsersNotifications_areForbidden() {
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.getNotifications(1L, 2L, 0, 10))));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.getUnread(1L, 2L))));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.markAllRead(1L, 2L))));
        verify(notificationService, never()).markAllAsRead(2L);
    }

    @Test
    @DisplayName("Ẩn danh -> 401")
    void anonymous_isUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.getUnread(null, 2L))));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.markRead(null, 5L))));
    }

    @Test
    @DisplayName("markRead chỉ thành công với thông báo của chính mình, ngược lại 404")
    void markRead_requiresOwnership() {
        when(notificationService.markAsRead(5L, 1L)).thenReturn(true);
        when(notificationService.markAsRead(6L, 1L)).thenReturn(false);

        assertEquals(HttpStatus.NO_CONTENT.value(), controller.markRead(1L, 5L).getStatusCode().value());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.markRead(1L, 6L))));
    }

    @Test
    @DisplayName("Chủ sở hữu đọc được thông báo của mình")
    void owner_canRead() {
        controller.getUnread(7L, 7L);
        verify(notificationService).getUnreadNotifications(7L);
    }
}
