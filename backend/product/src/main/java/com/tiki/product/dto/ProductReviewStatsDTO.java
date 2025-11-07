package com.tiki.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Review Statistics DTO from Review Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewStatsDTO {
    private Long productId;
    private Double averageRating;
    private Integer reviewCount;
}
