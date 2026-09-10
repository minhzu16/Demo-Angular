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
    private final com.tiki.warehouse.service.WarehouseAllocationService warehouseAllocationService;

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

    // ==========================================
    // Multi-Warehouse APIs (Giai đoạn Q4)
    // ==========================================

    @GetMapping("/locations")
    public ResponseEntity<List<com.tiki.warehouse.dto.WarehouseLocationDto>> getAllLocations() {
        log.info("Fetching all active warehouse locations");
        return ResponseEntity.ok(warehouseAllocationService.getAllLocations());
    }

    @PostMapping("/locations")
    public ResponseEntity<com.tiki.warehouse.dto.WarehouseLocationDto> createLocation(
            @jakarta.validation.Valid @RequestBody com.tiki.warehouse.dto.CreateWarehouseLocationRequest request) {
        log.info("Creating warehouse location: code={}", request.getCode());
        return ResponseEntity.ok(warehouseAllocationService.createLocation(request));
    }

    @GetMapping("/stock-by-warehouse/{productId}")
    public ResponseEntity<List<com.tiki.warehouse.entity.WarehouseStockEntity>> getStockByWarehouse(
            @PathVariable Long productId) {
        log.info("Fetching multi-warehouse stock for product: {}", productId);
        return ResponseEntity.ok(warehouseAllocationService.getStockByWarehouse(productId));
    }

    @PutMapping("/stock-by-warehouse/{warehouseId}/{productId}")
    public ResponseEntity<Void> updateWarehouseStock(
            @PathVariable Long warehouseId,
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        log.info("Updating warehouse {} stock for product {} to {}", warehouseId, productId, quantity);
        warehouseAllocationService.updateWarehouseStock(warehouseId, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/allocate")
    public ResponseEntity<com.tiki.warehouse.dto.WarehouseAllocationResponse> allocateFulfillment(
            @RequestBody com.tiki.warehouse.dto.WarehouseAllocationRequest request) {
        log.info("Allocating fulfillment for province: {}", request.getDestinationProvince());
        return ResponseEntity.ok(warehouseAllocationService.allocateFulfillment(request));
    }
}
