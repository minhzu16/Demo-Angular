package com.tiki.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service", url = "${order.service.url:http://order:8083}")
public interface OrderClient {

    @GetMapping("/api/v1/orders/stats/shop")
    Map<String, Object> getShopOrderStats(
        @RequestParam("shopId") Long shopId,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Username") String username
    );

    @GetMapping("/api/v1/orders/product/{productId}/sold-count")
    Integer getProductSoldCount(
        @PathVariable("productId") Long productId,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Username") String username
    );

    @GetMapping("/api/v1/orders/analytics/revenue")
    List<Map<String, Object>> getRevenueStats(
        @RequestParam("start") String start,
        @RequestParam("end") String end,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Username") String username
    );

    @GetMapping("/api/v1/orders/user/{userId}")
    List<Map<String, Object>> getOrdersByUser(
        @PathVariable("userId") Long userId,
        @RequestHeader("X-User-Id") String currentUserId,
        @RequestHeader("X-Username") String username,
        @RequestHeader("X-Role") String role
    );
}
