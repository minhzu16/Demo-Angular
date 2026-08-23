package com.tiki.warehouse.service;

import com.tiki.warehouse.dto.InventoryDto;
import com.tiki.warehouse.entity.InventoryEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseQueryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public Integer getStock(Long productId) {
        log.debug("Checking stock for product: {}", productId);
        return inventoryRepository.findByProductId(productId)
                .map(i -> {
                    int quantity = i.getQuantity() != null ? i.getQuantity() : 0;
                    int reserved = i.getReservedQuantity() != null ? i.getReservedQuantity() : 0;
                    return quantity - reserved;
                })
                .orElse(0);
    }

    public InventoryDto getInventory(Long productId) {
        log.debug("Fetching inventory info for product: {}", productId);
        return inventoryRepository.findByProductId(productId)
                .map(inventoryMapper::toDto)
                .orElse(null);
    }

    public List<InventoryDto> getShopStock(Long shopId) {
        log.debug("Fetching stock for shop: {}", shopId);
        return inventoryRepository.findByShopId(shopId)
                .stream()
                .map(inventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getShopStats(Long shopId) {
        log.debug("Fetching stats for shop: {}", shopId);
        List<InventoryEntity> items = inventoryRepository.findByShopId(shopId);
        long totalProducts = items.size();
        long lowStock = items.stream().filter(i -> i.getQuantity() != null && i.getQuantity() < 10 && i.getQuantity() > 0).count();
        long outOfStock = items.stream().filter(i -> i.getQuantity() == null || i.getQuantity() == 0).count();
        double totalValue = 0.0; // Requires pricing calculation mapping not in warehouse, returning 0
        return Map.of(
            "totalProducts", totalProducts,
            "lowStockProducts", lowStock,
            "outOfStockProducts", outOfStock,
            "totalValue", totalValue
        );
    }
}
