package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event published when flash sale stock changes
 * Phase 2 - Task 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleStockEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long flashSaleId;
    private Long productId;
    private Integer soldQuantity;
    private Integer totalQuantity;
}
