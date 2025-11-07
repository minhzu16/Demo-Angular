package com.tiki.product.controller;

import com.tiki.product.dto.PageResponseDTO;
import com.tiki.product.dto.ProductListDTO;
import com.tiki.product.service.ProductSearchService;
import com.tiki.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Search Controller using Elasticsearch
 * DISABLED: Elasticsearch not configured, using JPA search in ProductController instead
 */
@Slf4j
//@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
public class ProductSearchController {
    
    private final ProductSearchService searchService;
    private final ProductService productService;
    
    /**
     * Full-text search with filters
     * GET /api/products/search?q=laptop&category=1&brand=Dell&minPrice=1000&maxPrice=5000&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<ProductListDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        
        // Use keyword if q is null
        String searchQuery = q != null ? q : keyword;
        log.info("Search request: q={}, keyword={}, category={}, brand={}, minPrice={}, maxPrice={}", 
            q, keyword, category, brand, minPrice, maxPrice);
        
        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        
        try {
            Page<ProductListDTO> results = searchService.search(searchQuery, category, brand, minPrice, maxPrice, pageable);
            log.info("Found {} products (Elasticsearch)", results.getTotalElements());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable, falling back to JPA search: {}", e.getMessage());
            // Fallback to JPA search
            String sortParam = sortField + "," + (direction == Sort.Direction.DESC ? "desc" : "asc");
            PageResponseDTO<ProductListDTO> jpaResults = productService.search(searchQuery, category, brand, minPrice, maxPrice, sortParam, page, size);
            // Convert PageResponseDTO to Page
            org.springframework.data.domain.PageImpl<ProductListDTO> pageResults = 
                new org.springframework.data.domain.PageImpl<>(jpaResults.getContent(), pageable, jpaResults.getTotalElements());
            log.info("Found {} products (JPA)", pageResults.getTotalElements());
            return ResponseEntity.ok(pageResults);
        }
    }
    
    /**
     * Auto-complete suggestions
     * GET /api/products/search/suggestions?q=lap&limit=10
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Suggestions request: q={}, limit={}", q, limit);
        List<String> suggestions = searchService.getSuggestions(q, limit);
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * Search products by shop
     * GET /api/products/search/shop/1?q=laptop&page=0&size=20
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Page<ProductListDTO>> searchByShop(
            @PathVariable Integer shopId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Shop search request: shopId={}, q={}", shopId, q);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductListDTO> results = searchService.searchByShop(shopId, q, pageable);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Reindex all products (admin only)
     * POST /api/products/search/reindex
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> reindex() {
        log.info("Reindex request received");
        searchService.reindexAll();
        return ResponseEntity.ok("Reindex completed successfully");
    }
}
