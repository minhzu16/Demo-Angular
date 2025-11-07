package com.tiki.product.service;

import com.tiki.product.client.OrderClient;
import com.tiki.product.client.ReviewClient;
import com.tiki.product.client.ShopClient;
import com.tiki.product.document.ProductDocument;
import com.tiki.product.dto.ProductListDTO;
import com.tiki.product.dto.ProductReviewStatsDTO;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.repository.ProductRepository;
import com.tiki.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Search Service using Elasticsearch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    
    private final ProductSearchRepository searchRepository;
    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ShopClient shopClient;
    private final ReviewClient reviewClient;
    private final OrderClient orderClient;
    
    /**
     * Index a single product
     */
    public void indexProduct(ProductEntity product) {
        ProductDocument document = convertToDocument(product);
        searchRepository.save(document);
        log.info("Indexed product: {} - {}", product.getId(), product.getName());
    }
    
    /**
     * Index all products (for initial setup or reindex)
     */
    public void reindexAll() {
        log.info("Starting reindex of all products...");
        List<ProductEntity> allProducts = productRepository.findAll();
        
        List<ProductDocument> documents = allProducts.stream()
            .map(this::convertToDocument)
            .collect(Collectors.toList());
        
        searchRepository.saveAll(documents);
        log.info("Reindexed {} products", documents.size());
    }
    
    /**
     * Delete product from index
     */
    public void deleteFromIndex(Integer productId) {
        searchRepository.deleteById(productId);
        log.info("Deleted product from index: {}", productId);
    }
    
    /**
     * Full-text search with filters
     */
    public Page<ProductListDTO> search(String keyword, 
                                       Integer categoryId,
                                       String brand,
                                       BigDecimal minPrice,
                                       BigDecimal maxPrice,
                                       Pageable pageable) {
        
        Criteria criteria = new Criteria();
        
        // Keyword search (name or description)
        if (keyword != null && !keyword.isBlank()) {
            Criteria keywordCriteria = new Criteria("name").contains(keyword)
                .or(new Criteria("description").contains(keyword));
            criteria = criteria.and(keywordCriteria);
        }
        
        // Category filter
        if (categoryId != null) {
            criteria = criteria.and(new Criteria("categoryId").is(categoryId));
        }
        
        // Brand filter
        if (brand != null && !brand.isBlank()) {
            criteria = criteria.and(new Criteria("brand").is(brand));
        }
        
        // Price range filter
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }
        
        // Active products only
        criteria = criteria.and(new Criteria("status").is("ACTIVE"));
        
        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        
        List<ProductListDTO> results = searchHits.stream()
            .map(SearchHit::getContent)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(results, pageable, searchHits.getTotalHits());
    }
    
    /**
     * Auto-complete suggestions
     */
    public List<String> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.length() < 2) {
            return List.of();
        }
        
        Criteria criteria = new Criteria("name").contains(prefix)
            .and(new Criteria("status").is("ACTIVE"));
        
        Pageable pageable = PageRequest.of(0, limit);
        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        
        return hits.stream()
            .map(hit -> hit.getContent().getName())
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Search by shop
     */
    public Page<ProductListDTO> searchByShop(Integer shopId, String keyword, Pageable pageable) {
        Criteria criteria = new Criteria("shopId").is(shopId)
            .and(new Criteria("status").is("ACTIVE"));
        
        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.and(new Criteria("name").contains(keyword));
        }
        
        Query query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        
        List<ProductListDTO> results = hits.stream()
            .map(SearchHit::getContent)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }
    
    /**
     * Convert ProductEntity to ProductDocument
     * Enhanced with data from Shop, Review, Order services
     */
    private ProductDocument convertToDocument(ProductEntity entity) {
        // Get shop name from Shop Service
        String shopName = null;
        if (entity.getShopId() != null) {
            try {
                shopName = shopClient.getShopName(entity.getShopId());
            } catch (Exception e) {
                log.warn("Failed to fetch shop name for shopId {}: {}", entity.getShopId(), e.getMessage());
            }
        }
        
        // Get review statistics from Review Service
        Double rating = 0.0;
        Integer reviewCount = 0;
        try {
            ProductReviewStatsDTO reviewStats = reviewClient.getProductReviewStats(entity.getId().longValue());
            if (reviewStats != null) {
                rating = reviewStats.getAverageRating() != null ? reviewStats.getAverageRating() : 0.0;
                reviewCount = reviewStats.getReviewCount() != null ? reviewStats.getReviewCount() : 0;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch review stats for productId {}: {}", entity.getId(), e.getMessage());
        }
        
        // Get sold count from Order Service
        Integer soldCount = 0;
        try {
            soldCount = orderClient.getProductSoldCount(entity.getId().longValue());
            if (soldCount == null) soldCount = 0;
        } catch (Exception e) {
            log.warn("Failed to fetch sold count for productId {}: {}", entity.getId(), e.getMessage());
        }
        
        return ProductDocument.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .sku(entity.getSku())
            .brand(entity.getBrand())
            .price(entity.getPrice())
            .listPrice(entity.getListPrice())
            .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
            .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
            .shopId(entity.getShopId() != null ? entity.getShopId().intValue() : null)
            .shopName(shopName)
            .thumbnailUrl(entity.getThumbnailUrl())
            .status(entity.getStatus() != null ? entity.getStatus() : "ACTIVE")
            .rating(rating)
            .reviewCount(reviewCount)
            .soldCount(soldCount)
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getCreatedAt())
            .build();
    }
    
    /**
     * Convert ProductDocument to ProductListDTO
     */
    private ProductListDTO convertToDTO(ProductDocument doc) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(doc.getId());
        dto.setSku(doc.getSku());
        dto.setName(doc.getName());
        dto.setPrice(doc.getPrice());
        dto.setListPrice(doc.getListPrice());
        dto.setBrand(doc.getBrand());
        dto.setThumbnailUrl(doc.getThumbnailUrl());
        dto.setCategoryId(doc.getCategoryId());
        return dto;
    }
}
