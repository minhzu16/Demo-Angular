package com.tiki.product.inventory.repository;

import com.tiki.product.inventory.entity.InventoryTransactionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionItemRepository extends JpaRepository<InventoryTransactionItemEntity, Integer> {
}
