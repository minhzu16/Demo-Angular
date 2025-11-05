package com.tiki.analytics.controller;

import com.tiki.analytics.dto.ProductPerformanceDTO;
import com.tiki.analytics.service.ProductAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Product Analytics Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/products")
public class ProductAnalyticsController {
    
    // Service temporarily disabled
    // private final ProductAnalyticsService productAnalyticsService;
    
    /**
     * Get product performance
     */
    @GetMapping("/{productId}/performance")
    public ResponseEntity<?> getProductPerformance(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /products/{}/performance - days: {}", productId, days);
        // ProductPerformanceDTO performance = productAnalyticsService.getProductPerformance(productId, days);
        return ResponseEntity.ok(Map.of("productId", productId, "views", 0, "sales", 0));
    }
    
    /**
     * Get category performance by ID
     * GET /api/v1/analytics/products/category/{categoryId}/performance
     */
    @GetMapping("/category/{categoryId}/performance")
    public ResponseEntity<Map<String, Object>> getCategoryPerformanceById(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /products/category/{}/performance - days: {}", categoryId, days);
        
        // Mock data for now
        Map<String, Object> performance = Map.of(
            "categoryId", categoryId,
            "totalRevenue", 5000000,
            "totalOrders", 75,
            "totalProducts", 25,
            "averageRating", 4.5,
            "period", days + " days"
        );
        return ResponseEntity.ok(performance);
    }
    
    /**
     * Get category performance
     */
    @GetMapping("/category-performance")
    public ResponseEntity<List<Map<String, Object>>> getCategoryPerformance(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /category-performance - shopId: {}, days: {}", shopId, days);
        // List<Map<String, Object>> performance = productAnalyticsService.getCategoryPerformance(shopId, days);
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * Get wishlist trends
     */
    @GetMapping("/wishlist-trends")
    public ResponseEntity<Map<String, Object>> getWishlistTrends(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("GET /wishlist-trends - shopId: {}, days: {}", shopId, days);
        // Map<String, Object> trends = productAnalyticsService.getWishlistTrends(shopId, days);
        return ResponseEntity.ok(Map.of("shopId", shopId, "trends", List.of()));
    }
}
