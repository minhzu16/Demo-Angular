package com.tiki.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompareDto {
    private Long id;
    private Long userId;
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private String thumbnailUrl;
    private String brand;
    private LocalDateTime createdAt;
}
