package com.tiki.warehouse.repository;

import com.tiki.warehouse.entity.WarehouseLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, Long> {
    Optional<WarehouseLocationEntity> findByCode(String code);
    List<WarehouseLocationEntity> findByIsActiveTrue();
    List<WarehouseLocationEntity> findByProvinceIgnoreCase(String province);
    boolean existsByCode(String code);
}
