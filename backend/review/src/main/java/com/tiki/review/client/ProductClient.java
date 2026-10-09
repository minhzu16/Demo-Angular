package com.tiki.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Looks up which shop sells a product (used to authorize shop replies). */
@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:http://product:8081}")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductRef getProduct(@PathVariable("id") Long id);

    /** Only the field we need; unknown JSON properties are ignored. */
    record ProductRef(Long shopId) {}
}
