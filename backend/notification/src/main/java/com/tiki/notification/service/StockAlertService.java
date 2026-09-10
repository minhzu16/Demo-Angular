package com.tiki.notification.service;

import com.tiki.notification.entity.StockAlertEntity;
import com.tiki.notification.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final NotificationService notificationService;

    @Transactional
    public StockAlertEntity subscribe(Long userId, String userEmail, Long productId) {
        log.info("Subscribing stock alert: userId={}, productId={}", userId, productId);

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("ProductId không hợp lệ");
        }

        if (stockAlertRepository.existsByProductIdAndUserIdAndNotifiedFalse(productId, userId)) {
            log.info("User {} already subscribed to stock alert for product {}", userId, productId);
            return stockAlertRepository.findByProductIdAndNotifiedFalse(productId).stream()
                    .filter(a -> a.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        }

        StockAlertEntity entity = StockAlertEntity.builder()
                .userId(userId)
                .userEmail(userEmail)
                .productId(productId)
                .notified(false)
                .build();

        return stockAlertRepository.save(entity);
    }

    @Transactional
    public int triggerRestockAlert(Long productId) {
        log.info("Triggering restock alert for product {}", productId);

        List<StockAlertEntity> subscribers = stockAlertRepository.findByProductIdAndNotifiedFalse(productId);
        if (subscribers.isEmpty()) {
            log.info("No active subscribers for product {}", productId);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        for (StockAlertEntity sub : subscribers) {
            try {
                notificationService.sendNotification(
                        sub.getUserId(),
                        "Sản phẩm bạn theo dõi đã có hàng!",
                        "Sản phẩm #" + productId + " hiện đã có hàng tại kho Tiki. Nhanh tay đặt hàng ngay!",
                        "STOCK_ALERT",
                        "/products/" + productId
                );
                sub.setNotified(true);
                sub.setNotifiedAt(now);
            } catch (Exception e) {
                log.error("Failed to send restock notification to user {}: {}", sub.getUserId(), e.getMessage());
            }
        }

        stockAlertRepository.saveAll(subscribers);
        log.info("Successfully notified {} subscribers for product {}", subscribers.size(), productId);
        return subscribers.size();
    }

    @Transactional(readOnly = true)
    public List<StockAlertEntity> getMyAlerts(Long userId) {
        return stockAlertRepository.findByUserIdAndNotifiedFalseOrderByCreatedAtDesc(userId);
    }
}
