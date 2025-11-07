package com.tiki.product.repository;

import com.tiki.common.entity.WishlistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Wishlist Repository
 * Sprint 10 - Wishlist Feature
 */
@Repository
public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {
    
    /**
     * Find wishlist by user
     */
    Page<WishlistEntity> findByUserIdOrderByAddedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find by user and product
     */
    Optional<WishlistEntity> findByUserIdAndProductIdAndVariantId(
        Long userId, 
        Long productId, 
        Long variantId
    );
    
    /**
     * Check if product is in wishlist
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Count wishlist items for user
     */
    Long countByUserId(Long userId);
    
    /**
     * Delete by user and product
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Delete all wishlist items for user
     */
    void deleteByUserId(Long userId);
    
    /**
     * Get product IDs in wishlist
     */
    @Query("SELECT w.productId FROM WishlistEntity w WHERE w.userId = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);
}
