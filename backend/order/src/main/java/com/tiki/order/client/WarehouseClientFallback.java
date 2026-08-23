package com.tiki.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ✅ BUG 41 FIX: Circuit Breaker Fallback for WarehouseClient
 * When Warehouse service is down, return false (fail-fast) instead of hanging
 * the caller thread. This prevents cascading failures across microservices.
 */
@Slf4j
@Component
public class WarehouseClientFallback implements WarehouseClient {

    @Override
    public Boolean reserveStock(Long productId, Integer quantity) {
        log.warn("CIRCUIT BREAKER: Warehouse service is down. " +
                 "Cannot reserve stock for product {} x{}. Returning false.", productId, quantity);
        return false;
    }

    @Override
    public Boolean confirmOrder(Long productId, Integer quantity) {
        log.warn("CIRCUIT BREAKER: Warehouse service is down. " +
                 "Cannot confirm order for product {} x{}. Returning false.", productId, quantity);
        return false;
    }

    @Override
    public Boolean releaseStock(Long productId, Integer quantity) {
        log.warn("CIRCUIT BREAKER: Warehouse service is down. " +
                 "Cannot release stock for product {} x{}. Returning false.", productId, quantity);
        return false;
    }
}
