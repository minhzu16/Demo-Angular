package com.tiki.notification.controller;

import com.tiki.notification.entity.NotificationEntity;
import com.tiki.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Notifications are personal: the {userId} in the path must be the caller's own id, taken from the
 * gateway-validated X-User-Id header. Previously anyone (even anonymous) could read or mark any user's
 * notifications by changing the id.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;

    private static void requireSelf(Long callerId, Long pathUserId) {
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem thông báo");
        }
        if (!callerId.equals(pathUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xem thông báo của chính mình");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationEntity>> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireSelf(callerId, userId);
        log.info("Fetching notifications for user: {}", userId);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, Math.min(Math.max(size, 1), 100)));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationEntity>> getUnread(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long userId) {
        requireSelf(callerId, userId);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id) {
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        if (!notificationService.markAsRead(id, callerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo");
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllRead(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long userId) {
        requireSelf(callerId, userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
