package com.tiki.warehouse.repository;

import com.tiki.warehouse.entity.WarehouseStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStockEntity, Long> {

    Optional<WarehouseStockEntity> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<WarehouseStockEntity> findByProductId(Long productId);

    List<WarehouseStockEntity> findByWarehouseId(Long warehouseId);

    @Modifying
    @Query("UPDATE WarehouseStockEntity w SET w.reservedQuantity = COALESCE(w.reservedQuantity, 0) + :quantity " +
           "WHERE w.warehouseId = :warehouseId AND w.productId = :productId AND (COALESCE(w.quantity, 0) - COALESCE(w.reservedQuantity, 0)) >= :quantity")
    int tryReserveWarehouseStock(@Param("warehouseId") Long warehouseId, 
                                 @Param("productId") Long productId, 
                                 @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE WarehouseStockEntity w SET w.quantity = (CASE WHEN (COALESCE(w.quantity, 0) - :quantity) > 0 THEN (COALESCE(w.quantity, 0) - :quantity) ELSE 0 END), " +
           "w.reservedQuantity = (CASE WHEN (COALESCE(w.reservedQuantity, 0) - :quantity) > 0 THEN (COALESCE(w.reservedQuantity, 0) - :quantity) ELSE 0 END) " +
           "WHERE w.warehouseId = :warehouseId AND w.productId = :productId")
    int confirmWarehouseStock(@Param("warehouseId") Long warehouseId, 
                              @Param("productId") Long productId, 
                              @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE WarehouseStockEntity w SET w.reservedQuantity = (CASE WHEN (COALESCE(w.reservedQuantity, 0) - :quantity) > 0 THEN (COALESCE(w.reservedQuantity, 0) - :quantity) ELSE 0 END) " +
           "WHERE w.warehouseId = :warehouseId AND w.productId = :productId")
    int releaseWarehouseStock(@Param("warehouseId") Long warehouseId, 
                              @Param("productId") Long productId, 
                              @Param("quantity") Integer quantity);
}
