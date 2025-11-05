package com.tiki.analytics.controller;

import com.tiki.analytics.dto.RevenueDTO;
import com.tiki.analytics.dto.TopProductDTO;
import com.tiki.analytics.service.SalesAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Sales Analytics Controller
 * Provides REST API for sales analytics
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/sales")
public class SalesAnalyticsController {
    
    // Service temporarily disabled to avoid timeout
    // private final SalesAnalyticsService salesAnalyticsService;
    
    /**
     * Get sales overview
     * GET /api/v1/analytics/sales/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSalesOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long shopId) {
        
        log.info("GET /overview - startDate: {}, endDate: {}, shopId: {}", startDate, endDate, shopId);
        
        // Mock data for now
        Map<String, Object> overview = Map.of(
            "totalRevenue", 10000000,
            "totalOrders", 150,
            "averageOrderValue", 66667,
            "period", Map.of(
                "start", startDate.toString(),
                "end", endDate.toString()
            )
        );
        return ResponseEntity.ok(overview);
    }
    
    /**
     * Get sales by date range
     * GET /api/v1/analytics/sales/range
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getSalesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long shopId) {
        
        log.info("GET /range - startDate: {}, endDate: {}, shopId: {}", startDate, endDate, shopId);
        
        // Mock data for now
        Map<String, Object> range = Map.of(
            "sales", List.of(
                Map.of("date", startDate.toString(), "revenue", 500000, "orders", 10),
                Map.of("date", startDate.plusDays(1).toString(), "revenue", 600000, "orders", 12)
            ),
            "summary", Map.of(
                "totalRevenue", 1100000,
                "totalOrders", 22,
                "days", java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
            )
        );
        return ResponseEntity.ok(range);
    }
    
    /**
     * Get revenue by period
     */
    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueDTO>> getRevenue(
            @RequestParam Long shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "daily") String period) {
        
        log.info("GET /revenue - shopId: {}, start: {}, end: {}, period: {}", shopId, start, end, period);
        // List<RevenueDTO> revenue = salesAnalyticsService.getRevenueByPeriod(shopId, start, end, period);
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * Get top selling products
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /top-products - shopId: {}, limit: {}, days: {}", shopId, limit, days);
        
        if (shopId == null) {
            // Return mock data if no shopId
            return ResponseEntity.ok(List.of());
        }
        
        // List<TopProductDTO> products = salesAnalyticsService.getTopProducts(shopId, limit, days);
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * Get order statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /statistics - shopId: {}, days: {}", shopId, days);
        // Map<String, Object> stats = salesAnalyticsService.getOrderStatistics(shopId, days);
        return ResponseEntity.ok(Map.of("totalOrders", 0, "totalRevenue", 0));
    }
    
    /**
     * Get average order value
     */
    @GetMapping("/aov")
    public ResponseEntity<Map<String, Object>> getAverageOrderValue(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /aov - shopId: {}, days: {}", shopId, days);
        // BigDecimal aov = salesAnalyticsService.getAverageOrderValue(shopId, days);
        return ResponseEntity.ok(Map.of(
                "shopId", shopId,
                "days", days,
                "averageOrderValue", 0
        ));
    }
    
    /**
     * Get conversion rate
     */
    @GetMapping("/conversion")
    public ResponseEntity<Map<String, Object>> getConversionRate(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /conversion - shopId: {}, days: {}", shopId, days);
        // Double rate = salesAnalyticsService.getConversionRate(shopId, days);
        return ResponseEntity.ok(Map.of(
                "shopId", shopId,
                "days", days,
                "conversionRate", 0.0
        ));
    }
    
    /**
     * Get dashboard summary
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam Long shopId) {
        log.info("GET /dashboard - shopId: {}", shopId);
        // Map<String, Object> dashboard = salesAnalyticsService.getDashboardSummary(shopId);
        return ResponseEntity.ok(Map.of("shopId", shopId, "summary", "Mock data"));
    }
}
