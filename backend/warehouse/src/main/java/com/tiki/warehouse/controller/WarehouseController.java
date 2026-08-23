package com.tiki.warehouse.controller;

import com.tiki.warehouse.dto.InventoryDto;
import com.tiki.warehouse.service.WarehouseCommandService;
import com.tiki.warehouse.service.WarehouseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Slf4j
public class WarehouseController {
    private final WarehouseQueryService warehouseQueryService;
    private final WarehouseCommandService warehouseCommandService;

    @GetMapping("/stock/{productId}")
    public ResponseEntity<Integer> getStock(@PathVariable Long productId) {
        log.info("Checking stock for product: {}", productId);
        return ResponseEntity.ok(warehouseQueryService.getStock(productId));
    }

    @PostMapping("/reserve/{productId}/{quantity}")
    public ResponseEntity<Boolean> reserve(@PathVariable Long productId, @PathVariable Integer quantity) {
        log.info("Reserving stock: {} for product: {}", quantity, productId);
        return ResponseEntity.ok(warehouseCommandService.reserveStock(productId, quantity));
    }

    @PostMapping("/confirm/{productId}/{quantity}")
    public ResponseEntity<Boolean> confirmOrder(@PathVariable Long productId, @PathVariable Integer quantity) {
        log.info("Confirming order (deducting stock): {} for product: {}", quantity, productId);
        warehouseCommandService.confirmOrder(productId, quantity);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/release/{productId}/{quantity}")
    public ResponseEntity<Boolean> releaseStock(@PathVariable Long productId, @PathVariable Integer quantity) {
        log.info("Releasing stock: {} for product: {}", quantity, productId);
        warehouseCommandService.releaseStock(productId, quantity);
        return ResponseEntity.ok(true);
    }

    @PutMapping("/stock/{productId}/{newTotal}")
    public ResponseEntity<Void> updateStock(@PathVariable Long productId, @PathVariable Integer newTotal) {
        log.info("Updating stock for product {} to {}", productId, newTotal);
        warehouseCommandService.updateStock(productId, newTotal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shops/{shopId}/stats")
    public ResponseEntity<Map<String, Object>> getShopStats(@PathVariable Long shopId) {
        log.info("Fetching stats for shop: {}", shopId);
        return ResponseEntity.ok(warehouseQueryService.getShopStats(shopId));
    }

    @GetMapping("/shops/{shopId}/stock")
    public ResponseEntity<List<InventoryDto>> getShopStock(@PathVariable Long shopId) {
        log.info("Fetching stock for shop: {}", shopId);
        return ResponseEntity.ok(warehouseQueryService.getShopStock(shopId));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<InventoryDto> getProductStockInfo(@PathVariable Long productId) {
        log.info("Fetching stock info for product: {}", productId);
        return ResponseEntity.ok(warehouseQueryService.getInventory(productId));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<InventoryDto> updateProductStock(
            @PathVariable Long productId, @RequestBody Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        if (quantity != null) {
            log.info("Updating stock for product {} to {}", productId, quantity);
            warehouseCommandService.updateStock(productId, quantity);
        }
        return ResponseEntity.ok(warehouseQueryService.getInventory(productId));
    }
}
