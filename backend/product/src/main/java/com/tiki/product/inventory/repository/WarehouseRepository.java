package com.tiki.product.inventory.repository;

import com.tiki.product.inventory.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Integer> {
}
