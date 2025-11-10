package com.tiki.order.controller;

import com.tiki.order.dto.*;
import com.tiki.order.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;
    
    /**
     * Get shipping info for an order
     * GET /api/v1/shipping/order/{orderId}
     */
    @GetMapping("/v1/shipping/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getShippingInfoByOrder(@PathVariable Integer orderId) {
        // Return basic shipping info for the order
        Map<String, Object> shippingInfo = Map.of(
            "orderId", orderId,
            "shippingMethod", "STANDARD",
            "shippingFee", 30000,
            "estimatedDeliveryDays", 3,
            "status", "PENDING"
        );
        return ResponseEntity.ok(shippingInfo);
    }
    
    /**
     * Calculate shipping fee (v1 API)
     * POST /api/v1/shipping/calculate
     */
    @PostMapping("/v1/shipping/calculate")
    public ResponseEntity<Map<String, Object>> calculateShippingV1(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = Map.of(
            "shippingFee", 30000,
            "estimatedDeliveryDays", 3,
            "method", "STANDARD"
        );
        return ResponseEntity.ok(result);
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get all active shipping zones (Public)
     * GET /api/shipping/zones
     */
    @GetMapping("/shipping/zones")
    public ResponseEntity<List<ShippingZoneDTO>> getActiveZones() {
        List<ShippingZoneDTO> zones = shippingService.getActiveZones();
        return ResponseEntity.ok(zones);
    }

    /**
     * Calculate shipping fee for a province (Public)
     * GET /api/shipping/calculate?province=Hà Nội
     */
    @GetMapping("/shipping/calculate")
    public ResponseEntity<ShippingOptionDTO> calculateShipping(
            @RequestParam String province) {
        ShippingOptionDTO option = shippingService.calculateShipping(province);
        return ResponseEntity.ok(option);
    }

    /**
     * Calculate shipping with POST (Public)
     * POST /api/shipping/calculate
     */
    @PostMapping("/shipping/calculate")
    public ResponseEntity<ShippingOptionDTO> calculateShippingPost(
            @Valid @RequestBody CalculateShippingRequest request) {
        ShippingOptionDTO option = shippingService.calculateShipping(request);
        return ResponseEntity.ok(option);
    }

    /**
     * Get all available provinces (Public)
     * GET /api/shipping/provinces
     */
    @GetMapping("/shipping/provinces")
    public ResponseEntity<List<String>> getAvailableProvinces() {
        List<String> provinces = shippingService.getAvailableProvinces();
        return ResponseEntity.ok(provinces);
    }

    /**
     * Check if province is covered (Public)
     * GET /api/shipping/check?province=Hà Nội
     */
    @GetMapping("/shipping/check")
    public ResponseEntity<Map<String, Boolean>> checkProvince(
            @RequestParam String province) {
        boolean covered = shippingService.isProvinceCovered(province);
        Map<String, Boolean> response = new HashMap<>();
        response.put("covered", covered);
        return ResponseEntity.ok(response);
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Get all shipping zones (Admin)
     * GET /api/admin/shipping/zones
     */
    @GetMapping("/admin/shipping/zones")
    public ResponseEntity<List<ShippingZoneDTO>> getAllZones(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        
        List<ShippingZoneDTO> zones;
        if (activeOnly) {
            zones = shippingService.getActiveZones();
        } else {
            zones = shippingService.getAllZones();
        }
        return ResponseEntity.ok(zones);
    }

    /**
     * Get shipping zone by ID (Admin)
     * GET /api/admin/shipping/zones/{id}
     */
    @GetMapping("/admin/shipping/zones/{id}")
    public ResponseEntity<ShippingZoneDTO> getZoneById(@PathVariable Integer id) {
        ShippingZoneDTO zone = shippingService.getZoneById(id);
        return ResponseEntity.ok(zone);
    }

    /**
     * Create shipping zone (Admin)
     * POST /api/admin/shipping/zones
     */
    @PostMapping("/admin/shipping/zones")
    public ResponseEntity<ShippingZoneDTO> createZone(
            @Valid @RequestBody CreateShippingZoneRequest request) {
        ShippingZoneDTO zone = shippingService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(zone);
    }

    /**
     * Update shipping zone (Admin)
     * PUT /api/admin/shipping/zones/{id}
     */
    @PutMapping("/admin/shipping/zones/{id}")
    public ResponseEntity<ShippingZoneDTO> updateZone(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateShippingZoneRequest request) {
        ShippingZoneDTO zone = shippingService.updateZone(id, request);
        return ResponseEntity.ok(zone);
    }

    /**
     * Delete shipping zone (Admin)
     * DELETE /api/admin/shipping/zones/{id}
     */
    @DeleteMapping("/admin/shipping/zones/{id}")
    public ResponseEntity<Map<String, String>> deleteZone(@PathVariable Integer id) {
        shippingService.deleteZone(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Shipping zone deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate shipping zone (Admin - soft delete)
     * PATCH /api/admin/shipping/zones/{id}/deactivate
     */
    @PatchMapping("/admin/shipping/zones/{id}/deactivate")
    public ResponseEntity<ShippingZoneDTO> deactivateZone(@PathVariable Integer id) {
        ShippingZoneDTO zone = shippingService.deactivateZone(id);
        return ResponseEntity.ok(zone);
    }

    /**
     * Get shipping statistics (Admin)
     * GET /api/admin/shipping/statistics
     */
    @GetMapping("/admin/shipping/statistics")
    public ResponseEntity<ShippingStatisticsDTO> getStatistics() {
        ShippingStatisticsDTO stats = shippingService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get zones by max fee (Admin)
     * GET /api/admin/shipping/zones/by-fee?maxFee=50000
     */
    @GetMapping("/admin/shipping/zones/by-fee")
    public ResponseEntity<List<ShippingZoneDTO>> getZonesByMaxFee(
            @RequestParam java.math.BigDecimal maxFee) {
        List<ShippingZoneDTO> zones = shippingService.getZonesByMaxFee(maxFee);
        return ResponseEntity.ok(zones);
    }
}
