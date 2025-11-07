package com.tiki.product.inventory.repository;

import com.tiki.product.inventory.entity.WarehouseLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, Integer> {
}
