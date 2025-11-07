package com.tiki.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for Order Service
 */
@FeignClient(name = "order-service", url = "${services.order.url:http://localhost:8083}")
public interface OrderClient {
    
    /**
     * Get sold count for a product
     */
    @GetMapping("/api/v1/orders/product/{productId}/sold-count")
    Integer getProductSoldCount(@PathVariable("productId") Long productId);
}
