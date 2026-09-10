package com.tiki.notification.controller;

import com.tiki.notification.entity.StockAlertEntity;
import com.tiki.notification.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/stock-alert")
@RequiredArgsConstructor
@Slf4j
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestParam Long productId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để đăng ký nhận thông báo hàng về"));
        }

        log.info("Received stock alert subscription request: userId={}, productId={}", userId, productId);
        StockAlertEntity alert = stockAlertService.subscribe(userId, username, productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(alert);
    }

    @GetMapping("/my")
    public ResponseEntity<List<StockAlertEntity>> getMyAlerts(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(stockAlertService.getMyAlerts(userId));
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerRestockAlert(
            @RequestParam Long productId) {

        log.info("Trigger restock alerts for product {}", productId);
        int notifiedCount = stockAlertService.triggerRestockAlert(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "notifiedSubscribers", notifiedCount,
                "status", "SUCCESS"
        ));
    }
}
