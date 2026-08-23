package com.tiki.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import com.tiki.product.repository.FlashSaleRepository;
import com.tiki.product.repository.FlashSaleProductRepository;
import com.tiki.product.repository.ProductRepository;
import com.tiki.product.entity.FlashSale;
import com.tiki.product.entity.FlashSaleProduct;
import com.tiki.product.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Flash Sale Controller
 * Sprint 16: Flash Sales System
 * Provides REST API for flash sales management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/flash-sales")
public class FlashSaleController {

    @Autowired
    private FlashSaleRepository flashSaleRepository;
    
    @Autowired
    private FlashSaleProductRepository flashSaleProductRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Get all flash sales
     * GET /api/v1/flash-sales
     */
    @GetMapping
    public ResponseEntity<?> getAllFlashSales(
            @RequestParam(required = false) String status) {
        log.info("GET /flash-sales - status: {}", status);
        
        List<FlashSale> sales;
        if (status != null) {
            try {
                sales = flashSaleRepository.findByStatus(FlashSale.FlashSaleStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid status: " + status));
            }
        } else {
            sales = flashSaleRepository.findAll();
        }
        
        return ResponseEntity.ok(Map.of(
            "flashSales", sales,
            "total", sales.size()
        ));
    }
    
    /**
     * Get flash sale by ID
     * GET /api/v1/flash-sales/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFlashSaleById(@PathVariable Long id) {
        log.info("GET /flash-sales/{}", id);
        
        return flashSaleRepository.findById(id)
            .map(sale -> ResponseEntity.ok(sale))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get active flash sales
     * GET /api/v1/flash-sales/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        log.info("GET /flash-sales/active - Current system time: {}", now);
        
        List<FlashSale> actives = flashSaleRepository.findActiveFlashSales(now);
        if (actives.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "flashSales", List.of(),
                "total", 0,
                "message", "No active flash sales at the moment"
            ));
        }
        
        FlashSale active = actives.get(0);
        List<FlashSaleProduct> fp = flashSaleProductRepository.findByFlashSaleId(active.getId());
        
        List<Map<String, Object>> items = fp.stream().map(item -> {
            ProductEntity product = productRepository.findById(item.getProductId().intValue()).orElse(null);
            String imageUrl = "";
            String productName = "Unknown Product";
            
            if (product != null) {
                productName = product.getName();
                imageUrl = product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0).getUrl() : "";
            }
            
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", item.getProductId());
            map.put("name", productName);
            map.put("price", item.getSalePrice());
            map.put("originalPrice", item.getOriginalPrice());
            map.put("soldQuantity", item.getQuantitySold());
            map.put("totalQuantity", item.getQuantityLimit());
            map.put("imageUrl", imageUrl);
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "id", active.getId(),
            "name", active.getName(),
            "endTime", active.getEndTime().toString(),
            "items", items
        ));
    }
    
    /**
     * Get upcoming flash sales
     * GET /api/v1/flash-sales/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingFlashSales() {
        log.info("GET /flash-sales/upcoming");
        List<FlashSale> upcoming = flashSaleRepository.findUpcomingFlashSales(LocalDateTime.now());
        
        return ResponseEntity.ok(Map.of(
            "flashSales", upcoming,
            "total", upcoming.size()
        ));
    }
    
    /**
     * Create new flash sale
     * POST /api/v1/flash-sales
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createFlashSale(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestBody FlashSale request) {
        
        log.info("ADMIN action: Creating flash sale: {}", request.getName());
        FlashSale saved = flashSaleRepository.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    /**
     * Update flash sale
     * PUT /api/v1/flash-sales/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateFlashSale(
            @PathVariable Long id,
            @RequestBody FlashSale request) {
        
        log.info("ADMIN action: Updating flash sale {}", id);
        return flashSaleRepository.findById(id).map(existing -> {
            existing.setName(request.getName());
            existing.setDescription(request.getDescription());
            existing.setStartTime(request.getStartTime());
            existing.setEndTime(request.getEndTime());
            existing.setStatus(request.getStatus());
            return ResponseEntity.ok(flashSaleRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Delete flash sale
     * DELETE /api/v1/flash-sales/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFlashSale(@PathVariable Long id) {
        log.info("ADMIN action: Deleting flash sale {}", id);
        if (flashSaleRepository.existsById(id)) {
            flashSaleRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted", "id", id));
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get products in flash sale
     * GET /api/v1/flash-sales/{id}/products
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<?> getFlashSaleProducts(@PathVariable Long id) {
        List<FlashSaleProduct> fp = flashSaleProductRepository.findByFlashSaleId(id);
        return ResponseEntity.ok(Map.of(
            "flashSaleId", id,
            "products", fp,
            "total", fp.size()
        ));
    }
    
    /**
     * Add product to flash sale
     * POST /api/v1/flash-sales/{id}/products
     */
    @PostMapping("/{id}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProductToFlashSale(
            @PathVariable Long id,
            @RequestBody FlashSaleProduct request) {
        
        return flashSaleRepository.findById(id).map(sale -> {
            request.setFlashSale(sale);
            FlashSaleProduct saved = flashSaleProductRepository.save(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * End flash sale early
     * POST /api/v1/flash-sales/{id}/end
     */
    @PostMapping("/{id}/end")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> endFlashSale(@PathVariable Long id) {
        log.info("ADMIN action: Ending flash sale early {}", id);
        return flashSaleRepository.findById(id).map(sale -> {
            sale.setStatus(FlashSale.FlashSaleStatus.ENDED);
            sale.setEndTime(LocalDateTime.now());
            flashSaleRepository.save(sale);
            
            log.info("Flash sale {} ended early and status set to ENDED", id);
            
            return ResponseEntity.ok(Map.of(
                "message", "Flash sale ended early",
                "id", id,
                "status", sale.getStatus(),
                "endTime", sale.getEndTime()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeProductFromFlashSale(
            @PathVariable Long id,
            @PathVariable Long productId) {
        
        log.info("ADMIN action: Removing product {} from flash sale {}", productId, id);
        Optional<FlashSaleProduct> fp = flashSaleProductRepository.findByFlashSaleIdAndProductId(id, productId);
        
        if (fp.isPresent()) {
            flashSaleProductRepository.delete(fp.get());
            return ResponseEntity.ok(Map.of("message", "Product removed from flash sale"));
        }
        
        return ResponseEntity.notFound().build();
    }
}
