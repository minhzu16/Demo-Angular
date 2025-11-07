package com.tiki.product.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateVariantRequest {

    private String color;

    private String size;

    private BigDecimal price;

    @Min(value = 0, message = "Stock must be >= 0")
    private Integer stock;

    private String imageUrl;

    private Boolean isActive;
}
