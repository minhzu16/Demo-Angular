package com.tiki.order.repository;

import com.tiki.order.entity.ShippingZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingZoneRepository extends JpaRepository<ShippingZoneEntity, Integer> {

    /**
     * Find all active shipping zones
     */
    List<ShippingZoneEntity> findByIsActiveTrue();

    /**
     * Find shipping zone by name
     */
    Optional<ShippingZoneEntity> findByNameIgnoreCase(String name);

    /**
     * Check if zone name exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find zone that covers a specific province
     * Note: This uses JSON_CONTAINS which works in MySQL 5.7+
     */
    @Query(value = "SELECT * FROM shipping_zones " +
            "WHERE is_active = true " +
            "AND JSON_CONTAINS(provinces, JSON_QUOTE(:province)) " +
            "LIMIT 1", nativeQuery = true)
    Optional<ShippingZoneEntity> findByProvince(@Param("province") String province);

    /**
     * Find all zones ordered by fee
     */
    List<ShippingZoneEntity> findAllByOrderByFeeAsc();

    /**
     * Find zones with fee less than or equal to max fee
     */
    @Query("SELECT sz FROM ShippingZoneEntity sz " +
            "WHERE sz.isActive = true AND sz.fee <= :maxFee " +
            "ORDER BY sz.fee ASC")
    List<ShippingZoneEntity> findByMaxFee(@Param("maxFee") java.math.BigDecimal maxFee);

    /**
     * Count active zones
     */
    long countByIsActiveTrue();
}
