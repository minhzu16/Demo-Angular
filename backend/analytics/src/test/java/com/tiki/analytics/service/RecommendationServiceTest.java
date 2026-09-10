package com.tiki.analytics.service;

import com.tiki.analytics.client.OrderClient;
import com.tiki.analytics.client.ProductClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private OrderClient orderClient;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    @DisplayName("Frequently Bought Together - Gợi ý sản phẩm cùng danh mục và loại trừ sản phẩm hiện tại")
    void testFrequentlyBoughtTogether() {
        // Product 100 has categoryId = 5
        when(productClient.getProductById(100L)).thenReturn(Map.of(
                "id", 100L,
                "name", "iPhone 15 Pro",
                "categoryId", 5
        ));

        // Category 5 search returns product 100, 101 (AirPods), 102 (Ốp lưng)
        when(productClient.searchProducts(isNull(), eq(5), isNull(), isNull(), isNull(), eq(0), eq(8)))
                .thenReturn(Map.of(
                        "content", List.of(
                                Map.of("id", 100, "name", "iPhone 15 Pro"),
                                Map.of("id", 101, "name", "AirPods Pro"),
                                Map.of("id", 102, "name", "Ốp lưng Magsafe")
                        )
                ));

        List<Map<String, Object>> recommendations = recommendationService.getFrequentlyBoughtTogether(100L);

        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());
        assertEquals(101, recommendations.get(0).get("id"));
        assertEquals(102, recommendations.get(1).get("id"));
    }

    @Test
    @DisplayName("Category Best Sellers - Trả về sản phẩm bán chạy theo danh mục")
    void testCategoryBestSellers() {
        when(productClient.searchProducts(isNull(), eq(10), isNull(), isNull(), isNull(), eq(0), eq(5)))
                .thenReturn(Map.of(
                        "content", List.of(
                                Map.of("id", 201, "name", "Nồi chiên không dầu"),
                                Map.of("id", 202, "name", "Nồi cơm điện tử")
                        )
                ));

        List<Map<String, Object>> bestSellers = recommendationService.getCategoryBestSellers(10, 5);

        assertNotNull(bestSellers);
        assertEquals(2, bestSellers.size());
        assertEquals("Nồi chiên không dầu", bestSellers.get(0).get("name"));
    }
}
