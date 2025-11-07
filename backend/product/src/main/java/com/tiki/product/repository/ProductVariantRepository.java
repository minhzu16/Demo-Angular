package com.tiki.product.repository;

import com.tiki.product.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Integer> {

    /**
     * Find all variants by product ID
     */
    List<ProductVariantEntity> findByProductId(Integer productId);

    /**
     * Find active variants by product ID
     */
    List<ProductVariantEntity> findByProductIdAndIsActive(Integer productId, Boolean isActive);

    /**
     * Find variant by SKU
     */
    Optional<ProductVariantEntity> findBySku(String sku);

    /**
     * Check if SKU exists
     */
    boolean existsBySku(String sku);

    /**
     * Find variants by product ID and color
     */
    List<ProductVariantEntity> findByProductIdAndColor(Integer productId, String color);

    /**
     * Find variant by product ID, color and size
     */
    Optional<ProductVariantEntity> findByProductIdAndColorAndSize(Integer productId, String color, String size);

    /**
     * Count variants by product ID
     */
    long countByProductId(Integer productId);

    /**
     * Find variants with low stock
     */
    @Query("SELECT v FROM ProductVariantEntity v WHERE v.product.id = :productId AND v.stock < :threshold AND v.isActive = true")
    List<ProductVariantEntity> findLowStockVariants(@Param("productId") Integer productId, @Param("threshold") Integer threshold);

    /**
     * Get all unique colors for a product
     */
    @Query("SELECT DISTINCT v.color FROM ProductVariantEntity v WHERE v.product.id = :productId AND v.isActive = true")
    List<String> findDistinctColorsByProductId(@Param("productId") Integer productId);

    /**
     * Get all unique sizes for a product
     */
    @Query("SELECT DISTINCT v.size FROM ProductVariantEntity v WHERE v.product.id = :productId AND v.isActive = true")
    List<String> findDistinctSizesByProductId(@Param("productId") Integer productId);

    /**
     * Get available sizes for a specific color
     */
    @Query("SELECT v.size FROM ProductVariantEntity v WHERE v.product.id = :productId AND v.color = :color AND v.isActive = true AND v.stock > 0")
    List<String> findAvailableSizesByProductIdAndColor(@Param("productId") Integer productId, @Param("color") String color);
}
