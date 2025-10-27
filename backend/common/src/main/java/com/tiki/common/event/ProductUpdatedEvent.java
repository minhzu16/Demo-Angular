package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Updated Event
 * Sprint 9 - Day 7-9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer productId;
    private String name;
    private String description;
    private String sku;
    private String brand;
    private BigDecimal price;
    private BigDecimal listPrice;
    private Integer categoryId;
    private Integer shopId;
    private String action; // CREATED, UPDATED, DELETED
    private LocalDateTime timestamp;
}
