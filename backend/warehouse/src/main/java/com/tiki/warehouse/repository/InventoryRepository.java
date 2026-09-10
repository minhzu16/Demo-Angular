package com.tiki.warehouse.repository;

import com.tiki.warehouse.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {
    Optional<InventoryEntity> findByProductId(Long productId);
    List<InventoryEntity> findByShopId(Long shopId);

    /**
     * Atomic stock reservation query:
     * Guarantees zero overselling (race condition prevention) under high concurrency.
     */
    @Modifying
    @Query("UPDATE InventoryEntity i SET i.reservedQuantity = COALESCE(i.reservedQuantity, 0) + :quantity " +
           "WHERE i.productId = :productId AND (COALESCE(i.quantity, 0) - COALESCE(i.reservedQuantity, 0)) >= :quantity")
    int tryReserveStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * Atomic order confirmation query:
     * Safely deducts both total stock and reserved stock.
     */
    @Modifying
    @Query("UPDATE InventoryEntity i SET i.quantity = (CASE WHEN (COALESCE(i.quantity, 0) - :quantity) > 0 THEN (COALESCE(i.quantity, 0) - :quantity) ELSE 0 END), " +
           "i.reservedQuantity = (CASE WHEN (COALESCE(i.reservedQuantity, 0) - :quantity) > 0 THEN (COALESCE(i.reservedQuantity, 0) - :quantity) ELSE 0 END) " +
           "WHERE i.productId = :productId")
    int confirmStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * Atomic release stock query:
     * Safely releases reserved stock on order cancellation/refund.
     */
    @Modifying
    @Query("UPDATE InventoryEntity i SET i.reservedQuantity = (CASE WHEN (COALESCE(i.reservedQuantity, 0) - :quantity) > 0 THEN (COALESCE(i.reservedQuantity, 0) - :quantity) ELSE 0 END) " +
           "WHERE i.productId = :productId")
    int releaseReservedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
