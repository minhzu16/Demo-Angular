package com.tiki.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Performance DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceDTO {
    
    private Long productId;
    private String productName;
    private Integer viewCount;
    private Integer addToCartCount;
    private Integer wishlistCount;
    private Integer purchaseCount;
    private Double addToCartRate;
    private Double purchaseRate;
    private Integer stockLevel;
    private Double turnoverRate;
}
