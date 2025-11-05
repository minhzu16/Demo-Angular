package com.tiki.analytics.service;

import com.tiki.analytics.dto.RevenueDTO;
import com.tiki.analytics.dto.TopProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sales Analytics Service
 * Provides sales metrics and analytics for shops
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {
    
    /**
     * Get revenue by period
     * @param shopId Shop ID
     * @param startDate Start date
     * @param endDate End date
     * @param period Period type (daily, weekly, monthly)
     * @return List of revenue data
     */
    @Cacheable(value = "revenue", key = "#shopId + '_' + #startDate + '_' + #endDate + '_' + #period")
    public List<RevenueDTO> getRevenueByPeriod(Long shopId, LocalDate startDate, LocalDate endDate, String period) {
        log.info("Getting revenue for shop {} from {} to {}, period: {}", shopId, startDate, endDate, period);
        
        // Mock implementation - replace with actual DB queries
        List<RevenueDTO> revenues = new ArrayList<>();
        LocalDate current = startDate;
        Random random = new Random(shopId); // Deterministic for same shopId
        
        while (!current.isAfter(endDate)) {
            BigDecimal baseRevenue = new BigDecimal(10000 + random.nextInt(40000));
            int orderCount = 50 + random.nextInt(150);
            
            RevenueDTO dto = RevenueDTO.builder()
                    .date(current)
                    .revenue(baseRevenue)
                    .orderCount(orderCount)
                    .averageOrderValue(baseRevenue.divide(new BigDecimal(orderCount), 2, RoundingMode.HALF_UP))
                    .previousRevenue(baseRevenue.multiply(new BigDecimal("0.9")))
                    .build();
            
            dto.calculateGrowthRate();
            revenues.add(dto);
            
            // Move to next period
            current = switch (period.toLowerCase()) {
                case "weekly" -> current.plusWeeks(1);
                case "monthly" -> current.plusMonths(1);
                default -> current.plusDays(1);
            };
        }
        
        log.debug("Found {} revenue records", revenues.size());
        return revenues;
    }
    
    /**
     * Get top selling products
     * @param shopId Shop ID
     * @param limit Number of products to return
     * @param days Number of days to look back
     * @return List of top products
     */
    @Cacheable(value = "topProducts", key = "#shopId + '_' + #limit + '_' + #days")
    public List<TopProductDTO> getTopProducts(Long shopId, int limit, int days) {
        log.info("Getting top {} products for shop {} in last {} days", limit, shopId, days);
        
        // Mock implementation
        List<TopProductDTO> products = new ArrayList<>();
        Random random = new Random(shopId);
        
        for (int i = 0; i < limit; i++) {
            int soldCount = 100 - (i * 10) + random.nextInt(20);
            BigDecimal revenue = new BigDecimal(soldCount * (100 + random.nextInt(400)));
            
            TopProductDTO dto = TopProductDTO.builder()
                    .productId((long) (i + 1))
                    .productName("Product " + (i + 1))
                    .sku("SKU-" + String.format("%04d", i + 1))
                    .soldCount(soldCount)
                    .revenue(revenue)
                    .viewCount(soldCount * (10 + random.nextInt(20)))
                    .conversionRate(5.0 + random.nextDouble() * 10)
                    .rank(i + 1)
                    .build();
            
            products.add(dto);
        }
        
        log.debug("Found {} top products", products.size());
        return products;
    }
    
    /**
     * Get order statistics
     * @param shopId Shop ID
     * @param days Number of days to look back
     * @return Statistics map
     */
    @Cacheable(value = "orderStats", key = "#shopId + '_' + #days")
    public Map<String, Object> getOrderStatistics(Long shopId, int days) {
        log.info("Getting order statistics for shop {} in last {} days", shopId, days);
        
        // Mock implementation
        Random random = new Random(shopId);
        Map<String, Object> stats = new HashMap<>();
        
        int totalOrders = 500 + random.nextInt(1000);
        int completedOrders = (int) (totalOrders * 0.85);
        int cancelledOrders = (int) (totalOrders * 0.05);
        int pendingOrders = totalOrders - completedOrders - cancelledOrders;
        
        stats.put("totalOrders", totalOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("cancelledOrders", cancelledOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("completionRate", 85.0 + random.nextDouble() * 10);
        stats.put("averageProcessingTime", 2.0 + random.nextDouble() * 3);
        stats.put("period", days + " days");
        
        log.debug("Calculated order statistics: {}", stats);
        return stats;
    }
    
    /**
     * Get average order value
     * @param shopId Shop ID
     * @param days Number of days to look back
     * @return Average order value
     */
    @Cacheable(value = "aov", key = "#shopId + '_' + #days")
    public BigDecimal getAverageOrderValue(Long shopId, int days) {
        log.info("Getting AOV for shop {} in last {} days", shopId, days);
        
        // Mock implementation
        Random random = new Random(shopId);
        BigDecimal aov = new BigDecimal(200 + random.nextInt(300));
        
        log.debug("Calculated AOV: {}", aov);
        return aov;
    }
    
    /**
     * Get conversion rate
     * @param shopId Shop ID
     * @param days Number of days to look back
     * @return Conversion rate percentage
     */
    @Cacheable(value = "conversion", key = "#shopId + '_' + #days")
    public Double getConversionRate(Long shopId, int days) {
        log.info("Getting conversion rate for shop {} in last {} days", shopId, days);
        
        // Mock implementation
        Random random = new Random(shopId);
        double conversionRate = 5.0 + random.nextDouble() * 10;
        
        log.debug("Calculated conversion rate: {}%", conversionRate);
        return conversionRate;
    }
    
    /**
     * Get dashboard summary
     * @param shopId Shop ID
     * @return Dashboard data
     */
    @Cacheable(value = "dashboard", key = "#shopId")
    public Map<String, Object> getDashboardSummary(Long shopId) {
        log.info("Getting dashboard summary for shop {}", shopId);
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("revenue", getRevenueByPeriod(shopId, LocalDate.now().minusDays(30), LocalDate.now(), "daily")
                .stream()
                .map(RevenueDTO::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        dashboard.put("topProducts", getTopProducts(shopId, 5, 30));
        dashboard.put("orderStats", getOrderStatistics(shopId, 30));
        dashboard.put("aov", getAverageOrderValue(shopId, 30));
        dashboard.put("conversionRate", getConversionRate(shopId, 30));
        
        return dashboard;
    }
}
