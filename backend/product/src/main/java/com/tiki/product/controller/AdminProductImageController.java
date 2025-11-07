package com.tiki.product.controller;

import com.tiki.product.entity.ProductEntity;
import com.tiki.product.entity.ProductImageEntity;
import com.tiki.product.repository.ProductImageRepository;
import com.tiki.product.repository.ProductRepository;
import com.tiki.product.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/products/{id}/images")
public class AdminProductImageController {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final FileStorageService fileStorageService;

    public AdminProductImageController(ProductRepository productRepository,
                                       ProductImageRepository productImageRepository,
                                       FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> upload(@PathVariable Integer id, @RequestParam("file") MultipartFile file) throws IOException {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        String url = fileStorageService.save(file);
        ProductImageEntity img = new ProductImageEntity();
        img.setProduct(product);
        img.setUrl(url);
        img.setSortOrder(0);
        productImageRepository.save(img);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{imgId}")
    @Transactional
    public ResponseEntity<?> remove(@PathVariable Integer id, @PathVariable Integer imgId) {
        // Simple delete by id (file not removed from disk in this basic version)
        productImageRepository.deleteById(imgId);
        return ResponseEntity.noContent().build();
    }
}
