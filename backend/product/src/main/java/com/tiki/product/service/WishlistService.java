package com.tiki.product.service;

import com.tiki.common.entity.WishlistEntity;
import com.tiki.product.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Wishlist Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    
    /**
     * Get user's wishlist (paginated)
     */
    @Cacheable(value = "wishlist", key = "#userId + '_' + #pageable.pageNumber")
    public Page<WishlistEntity> getUserWishlist(Long userId, Pageable pageable) {
        log.info("Getting wishlist for user: {}", userId);
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId, pageable);
    }
    
    /**
     * Add product to wishlist
     */
    @Transactional
    @CacheEvict(value = "wishlist", key = "#userId + '_*'")
    public WishlistEntity addToWishlist(Long userId, Long productId, Long variantId, String notes) {
        log.info("Adding product {} to wishlist for user {}", productId, userId);
        
        // Check if already exists
        var existing = wishlistRepository.findByUserIdAndProductIdAndVariantId(
            userId, productId, variantId
        );
        
        if (existing.isPresent()) {
            log.info("Product already in wishlist");
            return existing.get();
        }
        
        // Create new wishlist item
        WishlistEntity wishlist = WishlistEntity.builder()
                .userId(userId)
                .productId(productId)
                .variantId(variantId)
                .notes(notes)
                .build();
        
        return wishlistRepository.save(wishlist);
    }
    
    /**
     * Remove from wishlist
     */
    @Transactional
    @CacheEvict(value = "wishlist", key = "#userId + '_*'")
    public void removeFromWishlist(Long userId, Long productId) {
        log.info("Removing product {} from wishlist for user {}", productId, userId);
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }
    
    /**
     * Check if product is in wishlist
     */
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    /**
     * Get wishlist count
     */
    @Cacheable(value = "wishlistCount", key = "#userId")
    public Long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
    
    /**
     * Clear all wishlist
     */
    @Transactional
    @CacheEvict(value = {"wishlist", "wishlistCount"}, key = "#userId + '_*'")
    public void clearWishlist(Long userId) {
        log.info("Clearing wishlist for user {}", userId);
        wishlistRepository.deleteByUserId(userId);
    }
    
    /**
     * Get product IDs in wishlist
     */
    public List<Long> getWishlistProductIds(Long userId) {
        return wishlistRepository.findProductIdsByUserId(userId);
    }
    
    /**
     * Add multiple products to wishlist
     */
    @Transactional
    @CacheEvict(value = "wishlist", key = "#userId + '_*'")
    public List<WishlistEntity> addMultipleToWishlist(Long userId, List<Long> productIds) {
        log.info("Adding {} products to wishlist for user {}", productIds.size(), userId);
        
        return productIds.stream()
                .map(productId -> addToWishlist(userId, productId, null, null))
                .toList();
    }
}
