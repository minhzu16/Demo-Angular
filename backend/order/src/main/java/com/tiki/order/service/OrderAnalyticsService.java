package com.tiki.order.service;

import com.tiki.order.dto.OrderStatsDTO;
import com.tiki.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticsService {

    private final OrderRepository orderRepository;

    /**
     * Get order statistics for a shop
     */
    public OrderStatsDTO getShopOrderStats(Long shopId) {
        log.info("Getting order stats for shop: {}", shopId);

        // Calculate start of today
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        // Get statistics from repository
        Integer todayOrders = orderRepository.countTodayOrders(startOfDay);
        BigDecimal todayRevenue = orderRepository.calculateTodayRevenue(startOfDay);
        Integer pendingOrders = orderRepository.countPendingOrders();

        // Get total stats
        Long totalOrders = orderRepository.count();

        return OrderStatsDTO.builder()
                .shopId(shopId)
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .pendingOrders(pendingOrders)
                .totalOrders(totalOrders.intValue())
                .totalRevenue(BigDecimal.ZERO)
                .build();
    }

    /**
     * Get sold count for a product
     * Note: This is a placeholder returning total orders count
     * In real implementation, need to sum quantities from order_items table
     */
    public Integer getProductSoldCount(Long productId) {
        log.info("Getting sold count for product: {}", productId);

        Integer totalOrders = orderRepository.getProductSoldCount();

        // Mock: assume each product sold in 10% of orders
        return totalOrders != null ? (int)(totalOrders * 0.1) : 0;
    }

    /**
     * Get revenue stats by date range
     */
    public List<Map<String, Object>> getRevenueByRange(LocalDateTime start, LocalDateTime end) {
        log.info("Getting revenue stats from {} to {}", start, end);
        List<Object[]> stats = orderRepository.getDailyRevenueStats(start, end);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : stats) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", row[0].toString());
            map.put("revenue", row[1]);
            map.put("orderCount", row[2]);
            result.add(map);
        }
        return result;
    }
}
