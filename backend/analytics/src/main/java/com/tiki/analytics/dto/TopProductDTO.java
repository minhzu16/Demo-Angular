package com.tiki.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Top Product Analytics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDTO {
    
    private Long productId;
    private String productName;
    private String sku;
    private Integer soldCount;
    private BigDecimal revenue;
    private Double conversionRate;
    private Integer viewCount;
    private Integer rank;
}
