package com.tiki.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for Shop Service
 */
@FeignClient(name = "shop-service", url = "${services.shop.url:http://localhost:8084}")
public interface ShopClient {
    
    /**
     * Get shop name by ID
     */
    @GetMapping("/api/v1/shops/{shopId}/name")
    String getShopName(@PathVariable("shopId") Long shopId);
}
