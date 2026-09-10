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
        log.info("Getting real order stats for shop: {}", shopId);

        // Calculate start of today
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        Integer todayOrders;
        BigDecimal todayRevenue;
        Integer pendingOrders;
        Long totalOrders;
        BigDecimal totalRevenue;

        if (shopId != null) {
            todayOrders = orderRepository.countTodayOrdersByShopId(shopId, startOfDay);
            todayRevenue = orderRepository.calculateTodayRevenueByShopId(shopId, startOfDay);
            pendingOrders = orderRepository.countPendingOrdersByShopId(shopId);
            totalOrders = orderRepository.countByShopId(shopId);
            totalRevenue = orderRepository.calculateTotalRevenueByShopId(shopId);
        } else {
            todayOrders = orderRepository.countTodayOrders(startOfDay);
            todayRevenue = orderRepository.calculateTodayRevenue(startOfDay);
            pendingOrders = orderRepository.countPendingOrders();
            totalOrders = orderRepository.count();
            totalRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
        }

        return OrderStatsDTO.builder()
                .shopId(shopId)
                .todayOrders(todayOrders != null ? todayOrders : 0)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .pendingOrders(pendingOrders != null ? pendingOrders : 0)
                .totalOrders(totalOrders != null ? totalOrders.intValue() : 0)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }

    /**
     * Get real sold count for a product from order_items table
     */
    public Integer getProductSoldCount(Long productId) {
        log.info("Getting accurate sold count for product: {}", productId);
        if (productId == null) {
            return 0;
        }
        Integer count = orderRepository.getProductSoldCountReal(productId);
        return count != null ? count : 0;
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
