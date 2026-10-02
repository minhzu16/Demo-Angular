package com.tiki.order.client;

import com.tiki.order.dto.ProductPricingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:http://localhost:8081}")
public interface ProductClient {

    @PostMapping("/api/v1/products/internal/pricing-batch")
    List<ProductPricingDto> getBatchPricing(@RequestBody List<Integer> productIds);

    @GetMapping("/api/v1/products/{id}")
    ProductPricingDto getProduct(@PathVariable("id") Integer id);
}
