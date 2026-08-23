package com.tiki.product.controller;

import com.tiki.product.dto.PageResponseDTO;
import com.tiki.product.dto.ProductDetailDTO;
import com.tiki.product.dto.ProductListDTO;
import com.tiki.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sellerId", required = false) Long sellerId
    ) {
        String searchQuery = q != null ? q : keyword;
        // ✅ BUG 47 FIX: Prevent OOM by clamping pagination size
        if (size > 100) size = 100;
        if (size < 1) size = 10;
        
        log.debug("Listing products - page: {}, size: {}, query: {}, sellerId: {}", page, size, searchQuery, sellerId);
        return productService.search(searchQuery, categoryId, brand, minPrice, maxPrice, sort, page, size, sellerId);
    }

    @PostMapping
    public ProductDetailDTO create(
            @RequestBody ProductDetailDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long sellerId
    ) {
        log.debug("Creating product for seller: {}", sellerId);
        return productService.create(request, sellerId);
    }

    @PutMapping("/{id}")
    public ProductDetailDTO update(
            @PathVariable Integer id,
            @RequestBody ProductDetailDTO request,
            @RequestHeader(value = "X-User-Id", required = false) Long sellerId
    ) {
        log.debug("Updating product {} for seller: {}", id, sellerId);
        return productService.update(id, request, sellerId);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) Long sellerId
    ) {
        log.debug("Deleting product {} for seller: {}", id, sellerId);
        productService.delete(id, sellerId);
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
