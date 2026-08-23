package com.tiki.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${ORDER_SERVICE_URL:http://order:8083}")
public interface OrderClient {
    
    @GetMapping("/api/v1/orders/internal/check-purchase/{productId}/{userId}")
    Boolean checkUserPurchasedProduct(@PathVariable("productId") Long productId, @PathVariable("userId") Long userId);
}
