package com.tiki.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseAllocationRequest {
    private String destinationProvince;
    private List<AllocationItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationItem {
        private Long productId;
        private Integer quantity;
    }
}
