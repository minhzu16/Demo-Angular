package com.tiki.product.controller;

import com.tiki.common.entity.WishlistEntity;
import com.tiki.product.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wishlist Controller
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WishlistController {
    
    private final WishlistService wishlistService;
    
    /**
     * Get user's wishlist (plural endpoint)
     * GET /api/v1/wishlists
     */
    @GetMapping("/api/v1/wishlists")
    public ResponseEntity<Page<WishlistEntity>> getWishlists(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WishlistEntity> wishlist = wishlistService.getUserWishlist(userId, pageable);
        return ResponseEntity.ok(wishlist);
    }
    
    /**
     * Get wishlist count (plural endpoint)
     * GET /api/v1/wishlists/count
     */
    @GetMapping("/api/v1/wishlists/count")
    public ResponseEntity<Map<String, Long>> getWishlistsCount(
            @RequestHeader("X-User-Id") Long userId) {
        
        Long count = wishlistService.getWishlistCount(userId);
        
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user's wishlist
     * GET /api/v1/wishlist?page=0&size=20
     */
    @GetMapping("/api/v1/wishlist")
    public ResponseEntity<Page<WishlistEntity>> getWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WishlistEntity> wishlist = wishlistService.getUserWishlist(userId, pageable);
        return ResponseEntity.ok(wishlist);
    }
    
    /**
     * Add product to wishlist
     * POST /api/v1/wishlist
     */
    @PostMapping("/api/v1/wishlist")
    public ResponseEntity<WishlistEntity> addToWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody AddToWishlistRequest request) {
        
        WishlistEntity wishlist = wishlistService.addToWishlist(
            userId,
            request.getProductId(),
            request.getVariantId(),
            request.getNotes()
        );
        
        return ResponseEntity.ok(wishlist);
    }
    
    /**
     * Remove from wishlist
     * DELETE /api/v1/wishlist/{productId}
     */
    @DeleteMapping("/api/v1/wishlist/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId) {
        
        wishlistService.removeFromWishlist(userId, productId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Removed from wishlist");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if product is in wishlist
     * GET /api/v1/wishlist/check/{productId}
     */
    @GetMapping("/api/v1/wishlist/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkInWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId) {
        
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("inWishlist", inWishlist);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get wishlist count
     * GET /api/v1/wishlist/count
     */
    @GetMapping("/api/v1/wishlist/count")
    public ResponseEntity<Map<String, Long>> getWishlistCount(
            @RequestHeader("X-User-Id") Long userId) {
        
        Long count = wishlistService.getWishlistCount(userId);
        
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Clear all wishlist
     * DELETE /api/v1/wishlist/clear
     */
    @DeleteMapping("/api/v1/wishlist/clear")
    public ResponseEntity<Map<String, Object>> clearWishlist(
            @RequestHeader("X-User-Id") Long userId) {
        
        wishlistService.clearWishlist(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Wishlist cleared");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Add multiple products
     * POST /api/v1/wishlist/bulk
     */
    @PostMapping("/api/v1/wishlist/bulk")
    public ResponseEntity<List<WishlistEntity>> addMultipleToWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody BulkAddRequest request) {
        
        List<WishlistEntity> wishlists = wishlistService.addMultipleToWishlist(
            userId,
            request.getProductIds()
        );
        
        return ResponseEntity.ok(wishlists);
    }
    
    // DTOs
    public static class AddToWishlistRequest {
        private Long productId;
        private Long variantId;
        private String notes;
        
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Long getVariantId() { return variantId; }
        public void setVariantId(Long variantId) { this.variantId = variantId; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    public static class BulkAddRequest {
        private List<Long> productIds;
        
        public List<Long> getProductIds() { return productIds; }
        public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
    }
}
