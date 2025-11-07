package com.tiki.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Product Extended Controller
 * Handles product images, variants, and category extensions
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class ProductExtendedController {
    
    /**
     * Get product images
     * GET /api/v1/products/{productId}/images
     */
    @GetMapping("/products/{productId}/images")
    public ResponseEntity<?> getProductImages(@PathVariable Long productId) {
        log.info("Getting images for product: {}", productId);
        
        List<Map<String, Object>> images = List.of(
            Map.of(
                "id", 1,
                "productId", productId,
                "url", "https://via.placeholder.com/800",
                "thumbnailUrl", "https://via.placeholder.com/200",
                "isPrimary", true,
                "order", 1
            ),
            Map.of(
                "id", 2,
                "productId", productId,
                "url", "https://via.placeholder.com/800/2",
                "thumbnailUrl", "https://via.placeholder.com/200/2",
                "isPrimary", false,
                "order", 2
            )
        );
        
        return ResponseEntity.ok(Map.of(
            "images", images,
            "total", images.size()
        ));
    }
    
    /**
     * Get image by ID
     * GET /api/v1/products/images/{imageId}
     */
    @GetMapping("/products/images/{imageId}")
    public ResponseEntity<?> getImageById(@PathVariable Long imageId) {
        log.info("Getting image: {}", imageId);
        
        Map<String, Object> image = Map.of(
            "id", imageId,
            "productId", 1,
            "url", "https://via.placeholder.com/800",
            "thumbnailUrl", "https://via.placeholder.com/200",
            "isPrimary", true,
            "order", 1
        );
        
        return ResponseEntity.ok(image);
    }
    
    /**
     * Get variant by ID
     * GET /api/v1/products/variants/{variantId}
     */
    @GetMapping("/products/variants/{variantId}")
    public ResponseEntity<?> getVariantById(@PathVariable Long variantId) {
        log.info("Getting variant: {}", variantId);
        
        Map<String, Object> variant = Map.of(
            "id", variantId,
            "productId", 1,
            "sku", "VAR-" + variantId,
            "name", "Variant " + variantId,
            "price", 299000,
            "stock", 100,
            "attributes", Map.of(
                "color", "Red",
                "size", "M"
            )
        );
        
        return ResponseEntity.ok(variant);
    }
    
    /**
     * Check variant stock
     * GET /api/v1/products/variants/{variantId}/stock
     */
    @GetMapping("/products/variants/{variantId}/stock")
    public ResponseEntity<?> checkVariantStock(@PathVariable Long variantId) {
        log.info("Checking stock for variant: {}", variantId);
        
        Map<String, Object> stock = Map.of(
            "variantId", variantId,
            "available", 100,
            "reserved", 5,
            "inStock", true,
            "canBackorder", false
        );
        
        return ResponseEntity.ok(stock);
    }
    
    /**
     * Get category subcategories
     * GET /api/v1/categories/{categoryId}/subcategories
     */
    @GetMapping("/categories/{categoryId}/subcategories")
    public ResponseEntity<?> getCategorySubcategories(@PathVariable Long categoryId) {
        log.info("Getting subcategories for category: {}", categoryId);
        
        List<Map<String, Object>> subcategories = List.of(
            Map.of(
                "id", categoryId * 10 + 1,
                "name", "Subcategory 1",
                "parentId", categoryId,
                "productCount", 10
            ),
            Map.of(
                "id", categoryId * 10 + 2,
                "name", "Subcategory 2",
                "parentId", categoryId,
                "productCount", 15
            )
        );
        
        return ResponseEntity.ok(Map.of(
            "subcategories", subcategories,
            "total", subcategories.size()
        ));
    }
    
    /**
     * Get category products count
     * GET /api/v1/categories/{categoryId}/products/count
     */
    @GetMapping("/categories/{categoryId}/products/count")
    public ResponseEntity<?> getCategoryProductsCount(@PathVariable Long categoryId) {
        log.info("Getting products count for category: {}", categoryId);
        
        return ResponseEntity.ok(Map.of(
            "categoryId", categoryId,
            "totalProducts", 50,
            "activeProducts", 45,
            "outOfStock", 5
        ));
    }
}
