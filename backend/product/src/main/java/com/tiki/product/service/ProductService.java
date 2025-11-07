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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Cacheable(cacheNames = "products-page", key = "T(java.util.Objects).hash(#q,#categoryId,#brand,#minPrice,#maxPrice,#sort,#page,#size)", unless = "#result == null")
    public PageResponseDTO<ProductListDTO> search(String q,
                                                  Integer categoryId,
                                                  String brand,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String sort,
                                                  int page,
                                                  int size) {
        Sort sortSpec = Sort.by("id");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0];
            boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("desc");
            sortSpec = desc ? Sort.by(field).descending() : Sort.by(field).ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        Page<ProductEntity> p = productRepository.search(q, categoryId, brand, minPrice, maxPrice, pageable);
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
    @CacheEvict(cacheNames = {"product-detail", "products-page"}, allEntries = true)
    public ProductDetailDTO create(ProductDetailDTO request) {
        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new BadRequestException("SKU already exists");
        }
        ProductEntity entity = new ProductEntity();
        applyDetailToEntity(request, entity);
        if (request.getCategoryId() != null) {
            Optional<CategoryEntity> cat = categoryRepository.findById(request.getCategoryId());
            cat.ifPresent(entity::setCategory);
        }
        ProductEntity saved = productRepository.save(entity);
        return toDetailDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"product-detail", "products-page"}, allEntries = true)
    public ProductDetailDTO update(Integer id, ProductDetailDTO request) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        if (!entity.getSku().equalsIgnoreCase(request.getSku()) && productRepository.existsBySkuIgnoreCase(request.getSku())) {
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
    @CacheEvict(cacheNames = {"product-detail", "products-page"}, allEntries = true)
    public void delete(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    private void applyDetailToEntity(ProductDetailDTO req, ProductEntity e) {
        e.setSku(req.getSku());
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setPrice(req.getPrice());
        e.setListPrice(req.getListPrice());
        e.setBrand(req.getBrand());
        e.setThumbnailUrl(req.getThumbnailUrl());
        e.setAttributesJson(req.getAttributesJson());
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
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}
