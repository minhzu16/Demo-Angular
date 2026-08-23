package com.tiki.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto {
    private Long id;
    private Long productId;
    private Long shopId;
    private Integer quantity;
    private Integer reservedQuantity;
    private String location;
    private LocalDateTime updatedAt;
}
