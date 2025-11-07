package com.tiki.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVariantDTO {
    private Integer id;
    private Integer productId;
    private String sku;
    private String color;
    private String size;
    private BigDecimal price;
    private BigDecimal effectivePrice;
    private Integer stock;
    private String imageUrl;
    private Boolean isActive;
    private Boolean inStock;
    private LocalDateTime createdAt;
}
