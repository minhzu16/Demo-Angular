package com.tiki.product.inventory.repository;

import com.tiki.product.inventory.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Integer> {
}
