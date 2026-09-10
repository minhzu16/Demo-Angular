package com.tiki.order.service;

import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final OrderRepository orderRepository;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudAssessment {
        private int score;
        private String riskLevel; // LOW, MEDIUM, HIGH
        private String reason;
    }

    public FraudAssessment assessOrderRisk(OrderEntity order) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (order == null) {
            return new FraudAssessment(0, "LOW", "Bình thường");
        }

        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

        // 1. Velocity Check (Đặt hàng liên tục trong 10 phút)
        if (order.getUserId() != null) {
            long recentOrders = orderRepository.countByUserIdAndCreatedAtAfter(order.getUserId(), tenMinutesAgo);
            if (recentOrders >= 2) {
                score += 45;
                reasons.add("RAPID_FIRE_ORDERS: Người dùng đặt >= 3 đơn trong 10 phút");
            }
        }

        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            long phoneOrders = orderRepository.countByCustomerPhoneAndCreatedAtAfter(order.getCustomerPhone(), tenMinutesAgo);
            if (phoneOrders >= 2) {
                score += 35;
                reasons.add("HIGH_VELOCITY_PHONE: Số điện thoại đặt liên tục trong 10 phút");
            }
        }

        // 2. High-Value First Order Check (Đơn hàng giá trị > 20 triệu từ tài khoản chưa từng mua)
        BigDecimal threshold = BigDecimal.valueOf(20_000_000);
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(threshold) >= 0) {
            long completedOrders = order.getUserId() != null 
                    ? orderRepository.countByUserIdAndStatus(order.getUserId(), OrderEntity.OrderStatus.DELIVERED)
                    : 0;
            if (completedOrders == 0) {
                score += 35;
                reasons.add("HIGH_VALUE_FIRST_ORDER: Đơn hàng > 20.000.000đ từ tài khoản chưa từng có đơn hoàn tất");
            }
        }

        // 3. Suspicious Shipping Data
        if (order.getCustomerPhone() != null) {
            String digitsOnly = order.getCustomerPhone().replaceAll("[^0-9]", "");
            if (digitsOnly.length() < 9 || digitsOnly.length() > 12) {
                score += 30;
                reasons.add("INVALID_PHONE_FORMAT: Số điện thoại không đúng chuẩn");
            }
        }

        if (order.getShippingAddress() != null && order.getShippingAddress().trim().length() < 5) {
            score += 25;
            reasons.add("SUSPICIOUS_SHORT_ADDRESS: Địa chỉ giao hàng quá ngắn");
        }

        // Determine Level
        String level;
        if (score >= 70) {
            level = "HIGH";
        } else if (score >= 35) {
            level = "MEDIUM";
        } else {
            level = "LOW";
        }

        String joinedReasons = reasons.isEmpty() ? "Bình thường" : String.join("; ", reasons);
        log.info("Fraud assessment for order {}: score={}, level={}, reasons={}", 
                order.getOrderNumber(), score, level, joinedReasons);

        return FraudAssessment.builder()
                .score(score)
                .riskLevel(level)
                .reason(joinedReasons)
                .build();
    }
}
