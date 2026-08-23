package com.tiki.analytics.service;

import com.tiki.analytics.client.OrderClient;
import com.tiki.analytics.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final OrderClient orderClient;
    private final ProductClient productClient;

    public List<Map<String, Object>> getRecommendations(Long userId) {
        log.info("Generating recommendations for user: {}", userId);

        try {
            // 1. Get user orders
            List<Map<String, Object>> userOrders = orderClient.getOrdersByUser(userId, "system", "system", "ROLE_ADMIN");
            
            // 2. Extract categories
            Set<Integer> topCategoryIds = new HashSet<>();
            if (userOrders != null && !userOrders.isEmpty()) {
                for (Map<String, Object> order : userOrders) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
                    if (items != null) {
                        for (Map<String, Object> item : items) {
                            // Ideally item contains categoryId, but if not we just use product info
                            // To simulate, we'll extract product IDs and fetch their categories,
                            // or just use popular ones if not available.
                            if (item.containsKey("categoryId") && item.get("categoryId") != null) {
                                topCategoryIds.add(Integer.parseInt(item.get("categoryId").toString()));
                            }
                        }
                    }
                }
            }

            if (topCategoryIds.isEmpty()) {
                topCategoryIds.addAll(Arrays.asList(1, 2, 3)); // Fallback
            }
            
            // 3. Search products in these categories
            List<Map<String, Object>> recommendations = new ArrayList<>();
            for (Integer catId : topCategoryIds) {
                Map<String, Object> response = productClient.searchProducts(null, catId, null, null, null, 0, 4);
                if (response != null && response.get("content") != null) {
                    List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("content");
                    recommendations.addAll(products);
                }
            }

            // 4. Shuffle and limit
            Collections.shuffle(recommendations);
            return recommendations.stream().limit(8).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to generate recommendations for user {}", userId, e);
            // Fallback to trending
            return getTrendingProducts();
        }
    }

    /**
     * Get trending products (global recommendations)
     */
    public List<Map<String, Object>> getTrendingProducts() {
        log.info("Fetching trending products");
        try {
            Map<String, Object> response = productClient.searchProducts(null, null, null, null, null, 0, 8);
            if (response != null && response.get("content") != null) {
                return (List<Map<String, Object>>) response.get("content");
            }
        } catch (Exception e) {
            log.error("Failed to fetch trending products", e);
        }
        return Collections.emptyList();
    }
}
