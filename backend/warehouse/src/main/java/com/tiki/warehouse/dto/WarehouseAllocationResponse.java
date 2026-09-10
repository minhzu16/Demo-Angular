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
public class WarehouseAllocationResponse {
    private boolean success;
    private Long allocatedWarehouseId;
    private String allocatedWarehouseCode;
    private String allocatedWarehouseName;
    private String message;
    private List<ItemAllocationDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemAllocationDetail {
        private Long productId;
        private Integer requestedQuantity;
        private Integer allocatedQuantity;
        private boolean reserved;
    }
}
