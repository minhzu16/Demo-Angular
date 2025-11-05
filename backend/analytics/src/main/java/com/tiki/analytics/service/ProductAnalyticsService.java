package com.tiki.analytics.service;

import com.tiki.analytics.dto.ProductPerformanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Product Analytics Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAnalyticsService {
    
    /**
     * Get product performance metrics
     */
    @Cacheable(value = "productPerformance", key = "#productId + '_' + #days")
    public ProductPerformanceDTO getProductPerformance(Long productId, int days) {
        log.info("Getting performance for product {} in last {} days", productId, days);
        
        // Mock implementation
        Random random = new Random(productId);
        int viewCount = 1000 + random.nextInt(5000);
        int addToCartCount = (int) (viewCount * 0.15);
        int purchaseCount = (int) (addToCartCount * 0.35);
        int wishlistCount = (int) (viewCount * 0.08);
        
        return ProductPerformanceDTO.builder()
                .productId(productId)
                .productName("Product " + productId)
                .viewCount(viewCount)
                .addToCartCount(addToCartCount)
                .wishlistCount(wishlistCount)
                .purchaseCount(purchaseCount)
                .addToCartRate((double) addToCartCount / viewCount * 100)
                .purchaseRate((double) purchaseCount / viewCount * 100)
                .stockLevel(50 + random.nextInt(200))
                .turnoverRate(15.0 + random.nextDouble() * 20)
                .build();
    }
    
    /**
     * Get category performance
     */
    @Cacheable(value = "categoryPerformance", key = "#shopId + '_' + #days")
    public List<Map<String, Object>> getCategoryPerformance(Long shopId, int days) {
        log.info("Getting category performance for shop {} in last {} days", shopId, days);
        
        List<Map<String, Object>> categories = new ArrayList<>();
        String[] categoryNames = {"Electronics", "Fashion", "Home & Garden", "Sports", "Books"};
        Random random = new Random(shopId);
        
        for (int i = 0; i < categoryNames.length; i++) {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryId", i + 1);
            category.put("categoryName", categoryNames[i]);
            category.put("productCount", 50 + random.nextInt(200));
            category.put("revenue", 10000 + random.nextInt(90000));
            category.put("orderCount", 100 + random.nextInt(400));
            category.put("growthRate", -5.0 + random.nextDouble() * 35);
            categories.add(category);
        }
        
        return categories;
    }
    
    /**
     * Get wishlist trends
     */
    @Cacheable(value = "wishlistTrends", key = "#shopId + '_' + #days")
    public Map<String, Object> getWishlistTrends(Long shopId, int days) {
        log.info("Getting wishlist trends for shop {} in last {} days", shopId, days);
        
        Random random = new Random(shopId);
        Map<String, Object> trends = new HashMap<>();
        trends.put("totalWishlisted", 500 + random.nextInt(2000));
        trends.put("wishlistToCart", 25.0 + random.nextDouble() * 15);
        trends.put("wishlistToPurchase", 10.0 + random.nextDouble() * 10);
        trends.put("growthRate", 5.0 + random.nextDouble() * 20);
        
        return trends;
    }
}
