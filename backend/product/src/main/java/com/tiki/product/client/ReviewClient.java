package com.tiki.product.client;

import com.tiki.product.dto.ProductReviewStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for Review Service
 */
@FeignClient(name = "review-service", url = "${services.review.url:http://localhost:8086}")
public interface ReviewClient {
    
    /**
     * Get review statistics for a product
     */
    @GetMapping("/api/v1/reviews/product/{productId}/stats")
    ProductReviewStatsDTO getProductReviewStats(@PathVariable("productId") Long productId);
}
