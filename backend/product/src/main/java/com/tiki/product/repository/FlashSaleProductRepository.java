package com.tiki.product.repository;

import com.tiki.product.entity.FlashSaleProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Flash Sale Product Repository
 * Sprint 16: Flash Sales System
 */
@Repository
public interface FlashSaleProductRepository extends JpaRepository<FlashSaleProduct, Long> {
    
    /**
     * Find all products in a flash sale
     */
    List<FlashSaleProduct> findByFlashSaleId(Long flashSaleId);
    
    /**
     * Find product in flash sale
     */
    Optional<FlashSaleProduct> findByFlashSaleIdAndProductId(Long flashSaleId, Long productId);
    
    /**
     * Find available products in flash sale
     */
    @Query("SELECT fsp FROM FlashSaleProduct fsp WHERE fsp.flashSale.id = :flashSaleId " +
           "AND fsp.quantitySold < fsp.quantityLimit")
    List<FlashSaleProduct> findAvailableProducts(Long flashSaleId);
    
    /**
     * Check if product exists in any active flash sale
     */
    @Query("SELECT fsp FROM FlashSaleProduct fsp " +
           "WHERE fsp.productId = :productId " +
           "AND fsp.flashSale.status = 'ACTIVE'")
    List<FlashSaleProduct> findActiveFlashSalesByProductId(Long productId);
}
