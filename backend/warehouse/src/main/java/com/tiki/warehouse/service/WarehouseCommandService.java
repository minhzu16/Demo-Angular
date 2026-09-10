package com.tiki.warehouse.service;

import com.tiki.warehouse.client.NotificationClient;
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
    private final NotificationClient notificationClient;

    @Transactional
    public boolean reserveStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            log.warn("Invalid reserveStock request: productId={}, quantity={}", productId, quantity);
            return false;
        }

        // Ensure inventory record exists
        inventoryRepository.findByProductId(productId).orElseGet(() -> 
            inventoryRepository.save(InventoryEntity.builder()
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .build())
        );

        // Atomic update prevents race condition / overselling under concurrent requests
        int updated = inventoryRepository.tryReserveStock(productId, quantity);
        if (updated > 0) {
            log.info("Successfully reserved {} items for product {}", quantity, productId);
            return true;
        }

        log.warn("Insufficient stock for product id {}. Request: {}", productId, quantity);
        return false;
    }

    @Transactional
    public void updateStock(Long productId, Integer newTotalQuantity) {
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> InventoryEntity.builder()
                        .productId(productId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());
        
        int oldQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int newQty = newTotalQuantity != null ? Math.max(0, newTotalQuantity) : 0;

        inventory.setQuantity(newQty);
        inventoryRepository.save(inventory);
        log.info("Updated stock for product {} to {}", productId, newTotalQuantity);

        // ✅ Q2: Khi hàng về (từ <= 0 lên > 0), bắn thông báo cho khách hàng đã đăng ký nhận tin
        if (oldQuantity <= 0 && newQty > 0) {
            try {
                if (notificationClient != null) {
                    notificationClient.triggerRestockAlert(productId);
                    log.info("Triggered restock alert for product {}", productId);
                }
            } catch (Exception e) {
                log.error("Failed to notify restock alert for product {}: {}", productId, e.getMessage());
            }
        }
    }
    
    @Transactional
    public void confirmOrder(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        int updated = inventoryRepository.confirmStock(productId, quantity);
        log.info("Order confirmed for product {}, deducted {}, rows updated: {}", productId, quantity, updated);
    }

    @Transactional
    public void releaseStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        int updated = inventoryRepository.releaseReservedStock(productId, quantity);
        log.info("Stock released for product {}, released {}, rows updated: {}", productId, quantity, updated);
    }
}
