package com.tiki.product.controller;

import com.tiki.product.dto.PageResponseDTO;
import com.tiki.product.dto.ProductDetailDTO;
import com.tiki.product.dto.ProductListDTO;
import com.tiki.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Product Controller - REST API for product management
 * Optimized with Lombok for cleaner code
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponseDTO<ProductListDTO> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) Integer categoryId,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        String searchQuery = q != null ? q : keyword;
        log.debug("Listing products - page: {}, size: {}, query: {}, keyword: {}", page, size, q, keyword);
        return productService.search(searchQuery, categoryId, brand, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/{id}")
    public ProductDetailDTO detail(@PathVariable Integer id) {
        log.debug("Getting product detail for id: {}", id);
        return productService.getDetail(id);
    }
    
    /**
     * Get product details (alternative endpoint)
     */
    @GetMapping("/{id}/details")
    public ProductDetailDTO getDetails(@PathVariable Integer id) {
        log.debug("Getting product details for id: {}", id);
        return productService.getDetail(id);
    }
    
    /**
     * Search products (dedicated search endpoint)
     */
    @GetMapping("/search")
    public PageResponseDTO<ProductListDTO> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        String searchQuery = keyword != null ? keyword : q;
        log.debug("Searching products with keyword: {}", searchQuery);
        return productService.search(searchQuery, categoryId, null, null, null, null, page, size);
    }
    
    /**
     * Get product variants
     */
    @GetMapping("/{id}/variants")
    public java.util.List<java.util.Map<String, Object>> getVariants(@PathVariable Integer id) {
        log.debug("Getting variants for product: {}", id);
        // Return empty list for now - can be enhanced later
        return java.util.List.of(
            java.util.Map.of(
                "id", 1,
                "productId", id,
                "name", "Default Variant",
                "price", 100000,
                "stock", 50
            )
        );
    }
}
