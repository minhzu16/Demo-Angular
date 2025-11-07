package com.tiki.product.service;

import com.tiki.product.dto.*;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.entity.ProductVariantEntity;
import com.tiki.product.exception.ResourceNotFoundException;
import com.tiki.product.repository.ProductRepository;
import com.tiki.product.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Get all variants for a product
     */
    public List<ProductVariantDTO> getVariantsByProductId(Integer productId) {
        List<ProductVariantEntity> variants = variantRepository.findByProductId(productId);
        return variants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active variants for a product
     */
    public List<ProductVariantDTO> getActiveVariantsByProductId(Integer productId) {
        List<ProductVariantEntity> variants = variantRepository.findByProductIdAndIsActive(productId, true);
        return variants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get variant by ID
     */
    public ProductVariantDTO getVariantById(Integer variantId) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
        return convertToDTO(variant);
    }

    /**
     * Get available variant options (colors and sizes)
     */
    public VariantOptionsDTO getVariantOptions(Integer productId) {
        List<String> colors = variantRepository.findDistinctColorsByProductId(productId);
        List<String> sizes = variantRepository.findDistinctSizesByProductId(productId);
        return new VariantOptionsDTO(colors, sizes);
    }

    /**
     * Get available sizes for a specific color
     */
    public List<String> getAvailableSizesForColor(Integer productId, String color) {
        return variantRepository.findAvailableSizesByProductIdAndColor(productId, color);
    }

    /**
     * Create a new variant
     */
    @Transactional
    public ProductVariantDTO createVariant(Integer productId, CreateVariantRequest request) {
        // Check if product exists
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Check if SKU already exists
        if (variantRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        }

        // Create variant entity
        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku(request.getSku());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setPrice(request.getPrice());
        variant.setStock(request.getStock());
        variant.setImageUrl(request.getImageUrl());
        variant.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        ProductVariantEntity savedVariant = variantRepository.save(variant);
        return convertToDTO(savedVariant);
    }

    /**
     * Update a variant
     */
    @Transactional
    public ProductVariantDTO updateVariant(Integer variantId, UpdateVariantRequest request) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        // Update fields if provided
        if (request.getColor() != null) {
            variant.setColor(request.getColor());
        }
        if (request.getSize() != null) {
            variant.setSize(request.getSize());
        }
        if (request.getPrice() != null) {
            variant.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            variant.setStock(request.getStock());
        }
        if (request.getImageUrl() != null) {
            variant.setImageUrl(request.getImageUrl());
        }
        if (request.getIsActive() != null) {
            variant.setIsActive(request.getIsActive());
        }

        ProductVariantEntity updatedVariant = variantRepository.save(variant);
        return convertToDTO(updatedVariant);
    }

    /**
     * Update variant stock
     */
    @Transactional
    public ProductVariantDTO updateVariantStock(Integer variantId, Integer stock) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        variant.setStock(stock);
        ProductVariantEntity updatedVariant = variantRepository.save(variant);
        return convertToDTO(updatedVariant);
    }

    /**
     * Delete a variant
     */
    @Transactional
    public void deleteVariant(Integer variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variant not found with id: " + variantId);
        }
        variantRepository.deleteById(variantId);
    }

    /**
     * Check if variant is available (in stock)
     */
    public boolean isVariantAvailable(Integer variantId) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
        return variant.isInStock();
    }

    /**
     * Get low stock variants for a product
     */
    public List<ProductVariantDTO> getLowStockVariants(Integer productId, Integer threshold) {
        List<ProductVariantEntity> variants = variantRepository.findLowStockVariants(productId, threshold);
        return variants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    private ProductVariantDTO convertToDTO(ProductVariantEntity variant) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setId(variant.getId());
        dto.setProductId(variant.getProduct().getId());
        dto.setSku(variant.getSku());
        dto.setColor(variant.getColor());
        dto.setSize(variant.getSize());
        dto.setPrice(variant.getPrice());
        dto.setEffectivePrice(variant.getEffectivePrice());
        dto.setStock(variant.getStock());
        dto.setImageUrl(variant.getImageUrl());
        dto.setIsActive(variant.getIsActive());
        dto.setInStock(variant.isInStock());
        dto.setCreatedAt(variant.getCreatedAt());
        return dto;
    }
}
