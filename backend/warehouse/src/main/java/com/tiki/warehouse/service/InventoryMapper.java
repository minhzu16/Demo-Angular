package com.tiki.warehouse.service;

import com.tiki.warehouse.dto.InventoryDto;
import com.tiki.warehouse.entity.InventoryEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryDto toDto(InventoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return InventoryDto.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .shopId(entity.getShopId())
                .quantity(entity.getQuantity())
                .reservedQuantity(entity.getReservedQuantity())
                .location(entity.getLocation())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
