package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Order Item DTO for events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private String imageUrl;
}
