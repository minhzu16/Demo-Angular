package com.tiki.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service", url = "${product.service.url:http://product:8081}")
public interface ProductClient {

    @GetMapping("/api/v1/categories")
    List<Map<String, Object>> getAllCategories();

    @GetMapping("/api/v1/products")
    Map<String, Object> searchProducts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer categoryId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String brand,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal minPrice,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal maxPrice,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size
    );
}
