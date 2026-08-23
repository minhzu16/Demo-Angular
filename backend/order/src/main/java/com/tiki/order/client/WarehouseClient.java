package com.tiki.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// ✅ BUG 41 FIX: fallback = WarehouseClientFallback.class prevents cascading failures
@FeignClient(name = "warehouse-service", url = "${WAREHOUSE_SERVICE_URL:http://localhost:8092}", fallback = WarehouseClientFallback.class)
public interface WarehouseClient {
    
    @PostMapping("/api/v1/warehouse/reserve/{productId}/{quantity}")
    Boolean reserveStock(@PathVariable("productId") Long productId, @PathVariable("quantity") Integer quantity);
    
    @PostMapping("/api/v1/warehouse/confirm/{productId}/{quantity}")
    Boolean confirmOrder(@PathVariable("productId") Long productId, @PathVariable("quantity") Integer quantity);

    @PostMapping("/api/v1/warehouse/release/{productId}/{quantity}")
    Boolean releaseStock(@PathVariable("productId") Long productId, @PathVariable("quantity") Integer quantity);
}
