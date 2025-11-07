package com.tiki.product.repository;

import com.tiki.product.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch Repository for Product Search
 * Sprint 9 - Day 4-6
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Integer> {
    
    /**
     * Search products by name (full-text search)
     */
    Page<ProductDocument> findByNameContaining(String name, Pageable pageable);
    
    /**
     * Search products by name or description
     */
    Page<ProductDocument> findByNameContainingOrDescriptionContaining(
        String name, String description, Pageable pageable);
    
    /**
     * Search by brand
     */
    Page<ProductDocument> findByBrand(String brand, Pageable pageable);
    
    /**
     * Search by category
     */
    Page<ProductDocument> findByCategoryId(Integer categoryId, Pageable pageable);
    
    /**
     * Search by shop
     */
    Page<ProductDocument> findByShopId(Integer shopId, Pageable pageable);
    
    /**
     * Search by price range
     */
    Page<ProductDocument> findByPriceBetween(
        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    /**
     * Search by status
     */
    Page<ProductDocument> findByStatus(String status, Pageable pageable);
    
    /**
     * Search by rating >= threshold
     */
    Page<ProductDocument> findByRatingGreaterThanEqual(Double rating, Pageable pageable);
}
