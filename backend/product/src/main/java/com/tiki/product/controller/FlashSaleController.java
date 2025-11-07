package com.tiki.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Flash Sale Controller
 * Sprint 16: Flash Sales System
 * Provides REST API for flash sales management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/flash-sales")
public class FlashSaleController {
    
    /**
     * Get all flash sales
     * GET /api/v1/flash-sales
     */
    @GetMapping
    public ResponseEntity<?> getAllFlashSales(
            @RequestParam(required = false) String status) {
        log.info("GET /flash-sales - status: {}", status);
        
        return ResponseEntity.ok(Map.of(
            "flashSales", List.of(
                Map.of(
                    "id", 1,
                    "name", "Mega Flash Sale 11.11",
                    "description", "Giảm giá cực sốc dịp 11.11",
                    "startTime", "2024-11-11T00:00:00",
                    "endTime", "2024-11-11T23:59:59",
                    "status", "ENDED"
                ),
                Map.of(
                    "id", 2,
                    "name", "Black Friday 2024",
                    "description", "Black Friday - Giảm đến 70%",
                    "startTime", "2024-11-29T00:00:00",
                    "endTime", "2024-11-29T23:59:59",
                    "status", "SCHEDULED"
                ),
                Map.of(
                    "id", 3,
                    "name", "Cyber Monday Sale",
                    "description", "Săn deal công nghệ",
                    "startTime", "2024-12-02T00:00:00",
                    "endTime", "2024-12-02T23:59:59",
                    "status", "SCHEDULED"
                )
            ),
            "total", 3
        ));
    }
    
    /**
     * Get flash sale by ID
     * GET /api/v1/flash-sales/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFlashSaleById(@PathVariable Long id) {
        log.info("GET /flash-sales/{}", id);
        
        return ResponseEntity.ok(Map.of(
            "id", id,
            "name", "Black Friday 2024",
            "description", "Black Friday - Giảm đến 70%",
            "startTime", "2024-11-29T00:00:00",
            "endTime", "2024-11-29T23:59:59",
            "status", "SCHEDULED",
            "productsCount", 2
        ));
    }
    
    /**
     * Get active flash sales
     * GET /api/v1/flash-sales/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveFlashSales() {
        log.info("GET /flash-sales/active");
        
        return ResponseEntity.ok(Map.of(
            "flashSales", List.of(),
            "total", 0,
            "message", "No active flash sales at the moment"
        ));
    }
    
    /**
     * Get upcoming flash sales
     * GET /api/v1/flash-sales/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingFlashSales() {
        log.info("GET /flash-sales/upcoming");
        
        return ResponseEntity.ok(Map.of(
            "flashSales", List.of(
                Map.of(
                    "id", 2,
                    "name", "Black Friday 2024",
                    "startTime", "2024-11-29T00:00:00",
                    "endTime", "2024-11-29T23:59:59",
                    "status", "SCHEDULED"
                ),
                Map.of(
                    "id", 3,
                    "name", "Cyber Monday Sale",
                    "startTime", "2024-12-02T00:00:00",
                    "endTime", "2024-12-02T23:59:59",
                    "status", "SCHEDULED"
                )
            ),
            "total", 2
        ));
    }
    
    /**
     * Create new flash sale
     * POST /api/v1/flash-sales
     */
    @PostMapping
    public ResponseEntity<?> createFlashSale(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestHeader(value = "X-Username", required = false) String adminUsername,
            @RequestBody Map<String, Object> request) {
        
        // ✅ SECURITY FIX: TODO - Add @PreAuthorize("hasRole('ADMIN')") when security is configured
        log.warn("ADMIN ACTION: User {} ({}) creating flash sale: {}", 
            adminUsername != null ? adminUsername : "unknown", adminId, request.get("name"));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", 4,
            "name", request.getOrDefault("name", "New Flash Sale"),
            "description", request.getOrDefault("description", ""),
            "startTime", request.getOrDefault("startTime", LocalDateTime.now().plusDays(1).toString()),
            "endTime", request.getOrDefault("endTime", LocalDateTime.now().plusDays(2).toString()),
            "status", "SCHEDULED",
            "message", "Flash sale created successfully (mock)"
        ));
    }
    
    /**
     * Update flash sale
     * PUT /api/v1/flash-sales/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFlashSale(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestHeader(value = "X-Username", required = false) String adminUsername,
            @RequestBody Map<String, Object> request) {
        
        // ✅ SECURITY FIX: TODO - Add @PreAuthorize("hasRole('ADMIN')") when security is configured
        log.warn("ADMIN ACTION: User {} ({}) updating flash sale {}", 
            adminUsername != null ? adminUsername : "unknown", adminId, id);
        
        return ResponseEntity.ok(Map.of(
            "id", id,
            "name", request.getOrDefault("name", "Updated Flash Sale"),
            "status", request.getOrDefault("status", "SCHEDULED"),
            "message", "Flash sale updated successfully (mock)"
        ));
    }
    
    /**
     * Delete flash sale
     * DELETE /api/v1/flash-sales/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFlashSale(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestHeader(value = "X-Username", required = false) String adminUsername) {
        
        // ✅ SECURITY FIX: TODO - Add @PreAuthorize("hasRole('ADMIN')") when security is configured
        log.error("ADMIN DELETE: User {} ({}) deleting flash sale {}", 
            adminUsername != null ? adminUsername : "unknown", adminId, id);
        
        return ResponseEntity.ok(Map.of(
            "message", "Flash sale deleted successfully",
            "id", id
        ));
    }
    
    /**
     * Get products in flash sale
     * GET /api/v1/flash-sales/{id}/products
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<?> getFlashSaleProducts(@PathVariable Long id) {
        log.info("GET /flash-sales/{}/products", id);
        
        return ResponseEntity.ok(Map.of(
            "flashSaleId", id,
            "products", List.of(
                Map.of(
                    "id", 1,
                    "productId", 1,
                    "originalPrice", 1000000,
                    "salePrice", 400000,
                    "discountPercentage", 60.0,
                    "quantityLimit", 200,
                    "quantitySold", 0,
                    "maxPerUser", 2,
                    "remaining", 200
                ),
                Map.of(
                    "id", 2,
                    "productId", 3,
                    "originalPrice", 800000,
                    "salePrice", 400000,
                    "discountPercentage", 50.0,
                    "quantityLimit", 150,
                    "quantitySold", 0,
                    "maxPerUser", 1,
                    "remaining", 150
                )
            ),
            "total", 2
        ));
    }
    
    /**
     * Add product to flash sale
     * POST /api/v1/flash-sales/{id}/products
     */
    @PostMapping("/{id}/products")
    public ResponseEntity<?> addProductToFlashSale(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestHeader(value = "X-Username", required = false) String adminUsername,
            @RequestBody Map<String, Object> request) {
        
        // ✅ SECURITY FIX: TODO - Add @PreAuthorize("hasRole('ADMIN')") when security is configured
        log.warn("ADMIN ACTION: User {} ({}) adding product {} to flash sale {}", 
            adminUsername != null ? adminUsername : "unknown", adminId, 
            request.get("productId"), id);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", 5,
            "flashSaleId", id,
            "productId", request.getOrDefault("productId", 1),
            "salePrice", request.getOrDefault("salePrice", 100000),
            "quantityLimit", request.getOrDefault("quantityLimit", 100),
            "message", "Product added to flash sale successfully (mock)"
        ));
    }
}
