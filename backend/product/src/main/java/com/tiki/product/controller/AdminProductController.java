package com.tiki.product.controller;

import com.tiki.product.dto.ProductDetailDTO;
import com.tiki.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/products")
@Slf4j
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * Get all products (admin)
     * GET /api/v1/admin/products
     * Hỗ trợ các tham số tìm kiếm giống FE: search, category, brand, page, size.
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand) {
        log.info("Admin getting all products (page={}, size={}, search={}, category={}, brand={})",
                page, size, search, category, brand);

        Integer categoryId = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryId = Integer.valueOf(category);
            } catch (NumberFormatException ex) {
                log.warn("Invalid category id filter: {}", category);
            }
        }

        com.tiki.product.dto.PageResponseDTO<com.tiki.product.dto.ProductListDTO> result =
                productService.search(
                        search,
                        categoryId,
                        brand,
                        null,
                        null,
                        null,
                        page,
                        size,
                        null
                );

        return ResponseEntity.ok(result);
    }
    
    /**
     * Get pending products (admin)
     * GET /api/v1/admin/products/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingProducts() {
        log.info("Admin getting pending products");
        
        return ResponseEntity.ok(Map.of(
            "products", List.of(),
            "total", 0,
            "message", "No pending products"
        ));
    }
    
    /**
     * Get product analytics (admin)
     * GET /api/v1/admin/products/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<?> getProductAnalytics() {
        log.info("Admin getting product analytics");
        
        Map<String, Object> analytics = Map.of(
            "totalProducts", 0,
            "activeProducts", 0,
            "pendingProducts", 0,
            "outOfStock", 0,
            "totalViews", 0,
            "totalSales", 0
        );
        
        return ResponseEntity.ok(analytics);
    }

    @PostMapping
    public ResponseEntity<ProductDetailDTO> create(@Valid @RequestBody ProductDetailDTO request) {
        return ResponseEntity.ok(productService.create(request, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDetailDTO> update(@PathVariable Integer id, @Valid @RequestBody ProductDetailDTO request) {
        return ResponseEntity.ok(productService.update(id, request, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.delete(id, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateProductStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        log.info("Admin updating product {} status to {}", id, status);
        productService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Product status updated successfully"));
    }
}
