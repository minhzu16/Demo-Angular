package com.tiki.product.service;

import com.tiki.product.dto.PageResponseDTO;
import com.tiki.product.dto.ProductDetailDTO;
import com.tiki.product.dto.ProductImageDTO;
import com.tiki.product.dto.ProductListDTO;
import com.tiki.product.entity.CategoryEntity;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.entity.ProductImageEntity;
import com.tiki.product.repository.CategoryRepository;
import com.tiki.product.repository.ProductImageRepository;
import com.tiki.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    @Cacheable(cacheNames = "products-page", key = "T(java.util.Objects).hash(#q,#categoryId,#brand,#minPrice,#maxPrice,#sort,#page,#size,#sellerId)", unless = "#result == null")
    public PageResponseDTO<ProductListDTO> search(String q,
            Integer categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            int page,
            int size,
            Long sellerId) {
            
        // Escape SQL wildcards to prevent wildcard DoS injections
        if (q != null) {
            q = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        }

        Sort sortSpec = Sort.by("id");
        if (sort != null && !sort.isBlank()) {
            String field;
            boolean desc;
            if (sort.contains(",")) {
                String[] parts = sort.split(",");
                field = parts[0].trim();
                desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
            } else if (sort.endsWith("_desc")) {
                field = sort.substring(0, sort.lastIndexOf("_desc"));
                desc = true;
            } else if (sort.endsWith("_asc")) {
                field = sort.substring(0, sort.lastIndexOf("_asc"));
                desc = false;
            } else {
                field = sort.trim();
                desc = false;
            }
            field = switch (field) {
                case "price" -> "price";
                case "name" -> "name";
                case "createdAt", "created" -> "id";
                case "rating" -> "averageRating"; 
                default -> "id";
            };
            sortSpec = desc ? Sort.by(field).descending() : Sort.by(field).ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        // We need a search method that also filters by sellerId
        Page<ProductEntity> p = productRepository.searchWithSeller(q, categoryId, brand, minPrice, maxPrice, sellerId, pageable);
        List<ProductListDTO> list = p.getContent().stream().map(this::toListDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(list, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Cacheable(cacheNames = "product-detail", key = "#id")
    public ProductDetailDTO getDetail(Integer id) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        List<ProductImageDTO> images = productImageRepository.findByProduct_IdOrderBySortOrderAsc(id)
                .stream().map(this::toImageDTO).collect(Collectors.toList());
        ProductDetailDTO dto = toDetailDTO(entity);
        dto.setImages(images);
        return dto;
    }

    @Transactional
    @CacheEvict(cacheNames = { "product-detail", "products-page" }, allEntries = true)
    public ProductDetailDTO create(ProductDetailDTO request, Long sellerId) {
        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new BadRequestException("SKU already exists");
        }
        ProductEntity entity = new ProductEntity();
        applyDetailToEntity(request, entity);
        entity.setSellerId(sellerId);
        entity.setShopId(request.getShopId());
        
        if (request.getCategoryId() != null) {
            Optional<CategoryEntity> cat = categoryRepository.findById(request.getCategoryId());
            cat.ifPresent(entity::setCategory);
        }
        ProductEntity saved = productRepository.save(entity);
        return toDetailDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = { "product-detail", "products-page" }, allEntries = true)
    public ProductDetailDTO update(Integer id, ProductDetailDTO request, Long sellerId) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        
        // Ensure seller owns the product (if sellerId is provided)
        if (sellerId != null && !sellerId.equals(entity.getSellerId())) {
            throw new BadRequestException("You don't have permission to update this product");
        }

        if (!entity.getSku().equalsIgnoreCase(request.getSku())
                && productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new BadRequestException("SKU already exists");
        }
        applyDetailToEntity(request, entity);
        if (request.getCategoryId() != null) {
            Optional<CategoryEntity> cat = categoryRepository.findById(request.getCategoryId());
            cat.ifPresent(entity::setCategory);
        } else {
            entity.setCategory(null);
        }
        ProductEntity saved = productRepository.save(entity);
        return toDetailDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = { "product-detail", "products-page" }, allEntries = true)
    public void delete(Integer id, Long sellerId) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        
        if (sellerId != null && !sellerId.equals(entity.getSellerId())) {
            throw new BadRequestException("You don't have permission to delete this product");
        }
        
        productRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(cacheNames = { "product-detail", "products-page" }, allEntries = true)
    public void updateStatus(Integer id, String status) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        entity.setStatus(status.toUpperCase());
        productRepository.save(entity);
    }

    private void applyDetailToEntity(ProductDetailDTO req, ProductEntity e) {
        e.setSku(req.getSku());
        
        // Basic XSS prevention (escaping HTML tags)
        if (req.getName() != null) {
            e.setName(req.getName().replace("<", "&lt;").replace(">", "&gt;"));
        }
        if (req.getDescription() != null) {
            e.setDescription(req.getDescription().replace("<", "&lt;").replace(">", "&gt;"));
        }
        
        e.setPrice(req.getPrice());
        e.setListPrice(req.getListPrice());
        e.setBrand(req.getBrand());
        e.setThumbnailUrl(req.getThumbnailUrl());
        e.setAttributesJson(req.getAttributesJson());
        e.setStock(req.getStock());
    }

    private ProductListDTO toListDTO(ProductEntity e) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(e.getId());
        dto.setSku(e.getSku());
        dto.setName(e.getName());
        dto.setPrice(e.getPrice());
        dto.setListPrice(e.getListPrice());
        dto.setBrand(e.getBrand());
        dto.setThumbnailUrl(e.getThumbnailUrl());
        dto.setCategoryId(e.getCategory() != null ? e.getCategory().getId() : null);
        dto.setAverageRating(e.getAverageRating());
        dto.setReviewCount(e.getReviewCount());
        dto.setStatus(e.getStatus());
        return dto;
    }

    private ProductDetailDTO toDetailDTO(ProductEntity e) {
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(e.getId());
        dto.setSku(e.getSku());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setPrice(e.getPrice());
        dto.setListPrice(e.getListPrice());
        dto.setBrand(e.getBrand());
        dto.setThumbnailUrl(e.getThumbnailUrl());
        dto.setAttributesJson(e.getAttributesJson());
        dto.setCategoryId(e.getCategory() != null ? e.getCategory().getId() : null);
        dto.setAverageRating(e.getAverageRating());
        dto.setReviewCount(e.getReviewCount());
        dto.setSellerId(e.getSellerId());
        dto.setShopId(e.getShopId());
        dto.setStock(e.getStock());
        dto.setStatus(e.getStatus());
        return dto;
    }

    private ProductImageDTO toImageDTO(ProductImageEntity e) {
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(e.getId());
        dto.setUrl(e.getUrl());
        dto.setSortOrder(e.getSortOrder());
        return dto;
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
