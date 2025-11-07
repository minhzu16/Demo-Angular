package com.tiki.product.controller;

import com.tiki.product.dto.*;
import com.tiki.product.service.ProductVariantService;
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
public class ProductVariantController {

    @Autowired
    private ProductVariantService variantService;

    /**
     * Get all variants for a product (Public)
     * GET /api/products/{productId}/variants
     */
    @GetMapping("/products/{productId}/variants")
    public ResponseEntity<List<ProductVariantDTO>> getProductVariants(
            @PathVariable Integer productId,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        
        List<ProductVariantDTO> variants;
        if (activeOnly) {
            variants = variantService.getActiveVariantsByProductId(productId);
        } else {
            variants = variantService.getVariantsByProductId(productId);
        }
        return ResponseEntity.ok(variants);
    }

    /**
     * Get variant options (available colors and sizes)
     * GET /api/products/{productId}/variant-options
     */
    @GetMapping("/products/{productId}/variant-options")
    public ResponseEntity<VariantOptionsDTO> getVariantOptions(@PathVariable Integer productId) {
        VariantOptionsDTO options = variantService.getVariantOptions(productId);
        return ResponseEntity.ok(options);
    }

    /**
     * Get available sizes for a specific color
     * GET /api/products/{productId}/variants/sizes?color={color}
     */
    @GetMapping("/products/{productId}/variants/sizes")
    public ResponseEntity<List<String>> getAvailableSizes(
            @PathVariable Integer productId,
            @RequestParam String color) {
        List<String> sizes = variantService.getAvailableSizesForColor(productId, color);
        return ResponseEntity.ok(sizes);
    }

    /**
     * Get variant by ID (Public)
     * GET /api/variants/{variantId}
     */
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ProductVariantDTO> getVariantById(@PathVariable Integer variantId) {
        ProductVariantDTO variant = variantService.getVariantById(variantId);
        return ResponseEntity.ok(variant);
    }

    /**
     * Check if variant is available
     * GET /api/variants/{variantId}/available
     */
    @GetMapping("/variants/{variantId}/available")
    public ResponseEntity<Map<String, Boolean>> checkVariantAvailability(@PathVariable Integer variantId) {
        boolean available = variantService.isVariantAvailable(variantId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new variant (Admin only)
     * POST /api/admin/products/{productId}/variants
     */
    @PostMapping("/admin/products/{productId}/variants")
    public ResponseEntity<ProductVariantDTO> createVariant(
            @PathVariable Integer productId,
            @Valid @RequestBody CreateVariantRequest request) {
        ProductVariantDTO variant = variantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(variant);
    }

    /**
     * Update a variant (Admin only)
     * PUT /api/admin/variants/{variantId}
     */
    @PutMapping("/admin/variants/{variantId}")
    public ResponseEntity<ProductVariantDTO> updateVariant(
            @PathVariable Integer variantId,
            @Valid @RequestBody UpdateVariantRequest request) {
        ProductVariantDTO variant = variantService.updateVariant(variantId, request);
        return ResponseEntity.ok(variant);
    }

    /**
     * Update variant stock (Admin only)
     * PATCH /api/admin/variants/{variantId}/stock
     */
    @PatchMapping("/admin/variants/{variantId}/stock")
    public ResponseEntity<ProductVariantDTO> updateVariantStock(
            @PathVariable Integer variantId,
            @RequestParam Integer stock) {
        ProductVariantDTO variant = variantService.updateVariantStock(variantId, stock);
        return ResponseEntity.ok(variant);
    }

    /**
     * Delete a variant (Admin only)
     * DELETE /api/admin/variants/{variantId}
     */
    @DeleteMapping("/admin/variants/{variantId}")
    public ResponseEntity<Map<String, String>> deleteVariant(@PathVariable Integer variantId) {
        variantService.deleteVariant(variantId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Variant deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get low stock variants (Admin only)
     * GET /api/admin/products/{productId}/variants/low-stock
     */
    @GetMapping("/admin/products/{productId}/variants/low-stock")
    public ResponseEntity<List<ProductVariantDTO>> getLowStockVariants(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<ProductVariantDTO> variants = variantService.getLowStockVariants(productId, threshold);
        return ResponseEntity.ok(variants);
    }
}
