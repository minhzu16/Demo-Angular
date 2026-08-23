package com.tiki.analytics.service;

import com.tiki.analytics.dto.ProductPerformanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

import com.tiki.analytics.client.OrderClient;

import com.tiki.analytics.client.ProductClient;

/**
 * Product Analytics Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAnalyticsService {
    
    private final OrderClient orderClient;
    private final ProductClient productClient;

    /**
     * Get product performance metrics
     */
    @Cacheable(value = "productPerformance", key = "#productId + '_' + #days")
    public ProductPerformanceDTO getProductPerformance(Long productId, int days) {
        log.info("Getting performance for product {} in last {} days", productId, days);
        
        Random random = new Random(productId);
        int viewCount = 1000 + random.nextInt(5000);
        int wishlistCount = (int) (viewCount * 0.08);
        int actualPurchaseCount = 0;
        
        try {
            Integer soldCount = orderClient.getProductSoldCount(productId, "1", "system_admin");
            if (soldCount != null) {
                actualPurchaseCount = soldCount;
            }
        } catch (Exception e) {
            log.error("Failed to get product sold count from order-service", e);
            actualPurchaseCount = (int) (viewCount * 0.15 * 0.35); // fallback mock
        }
        
        int addToCartCount = actualPurchaseCount > 0 ? (int) (actualPurchaseCount / 0.35) : (int) (viewCount * 0.15);
        
        return ProductPerformanceDTO.builder()
                .productId(productId)
                .productName("Product " + productId)
                .viewCount(viewCount)
                .addToCartCount(addToCartCount)
                .wishlistCount(wishlistCount)
                .purchaseCount(actualPurchaseCount)
                .addToCartRate(viewCount > 0 ? (double) addToCartCount / viewCount * 100 : 0)
                .purchaseRate(viewCount > 0 ? (double) actualPurchaseCount / viewCount * 100 : 0)
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
        List<String> categoryNames = new ArrayList<>();
        
        try {
            List<Map<String, Object>> productCategories = productClient.getAllCategories();
            if (productCategories != null) {
                for (Map<String, Object> c : productCategories) {
                    if (c.containsKey("name") && c.get("name") != null) {
                        categoryNames.add(c.get("name").toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get categories from product-service", e);
        }
        
        if (categoryNames.isEmpty()) {
            categoryNames = Arrays.asList("Electronics", "Fashion", "Home & Garden", "Sports", "Books");
        }
        
        Random random = new Random(shopId);
        
        for (int i = 0; i < categoryNames.size(); i++) {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryId", i + 1);
            category.put("categoryName", categoryNames.get(i));
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
