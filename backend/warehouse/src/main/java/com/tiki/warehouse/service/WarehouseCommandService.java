package com.tiki.warehouse.service;

import com.tiki.warehouse.entity.InventoryEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseCommandService {
    private final InventoryRepository inventoryRepository;

    @Transactional
    public boolean reserveStock(Long productId, Integer quantity) {
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> inventoryRepository.save(InventoryEntity.builder()
                        .productId(productId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build()));
        
        int available = (inventory.getQuantity() != null ? inventory.getQuantity() : 0) 
                      - (inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0);
        if (available >= quantity) {
            int currentReserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
            inventory.setReservedQuantity(currentReserved + quantity);
            inventoryRepository.save(inventory);
            log.info("Reserved {} items for product {}", quantity, productId);
            return true;
        }
        log.warn("Insufficient stock for product id {}. Request: {}, Available: {}", productId, quantity, available);
        return false;
    }

    @Transactional
    public void updateStock(Long productId, Integer newTotalQuantity) {
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> InventoryEntity.builder()
                        .productId(productId)
                        .reservedQuantity(0)
                        .build());
        
        inventory.setQuantity(newTotalQuantity);
        inventoryRepository.save(inventory);
        log.info("Updated stock for product {} to {}", productId, newTotalQuantity);
    }
    
    @Transactional
    public void confirmOrder(Long productId, Integer quantity) {
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
        
        int currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int currentReserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
        
        inventory.setQuantity(Math.max(0, currentQuantity - quantity));
        inventory.setReservedQuantity(Math.max(0, currentReserved - quantity));
        inventoryRepository.save(inventory);
        log.info("Order confirmed, subtracted {} from absolute stock of product {}", quantity, productId);
    }

    @Transactional
    public void releaseStock(Long productId, Integer quantity) {
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
                
        int currentReserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
        
        inventory.setReservedQuantity(Math.max(0, currentReserved - quantity));
        inventoryRepository.save(inventory);
        log.info("Stock released, subtracted {} from reserved stock of product {}", quantity, productId);
    }
}
