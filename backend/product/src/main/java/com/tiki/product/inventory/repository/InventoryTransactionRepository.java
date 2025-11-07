package com.tiki.product.inventory.repository;

import com.tiki.product.inventory.entity.InventoryTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, Integer> {
}
