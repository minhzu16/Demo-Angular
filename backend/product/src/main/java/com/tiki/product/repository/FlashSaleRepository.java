package com.tiki.product.repository;

import com.tiki.product.entity.FlashSale;
import com.tiki.product.entity.FlashSale.FlashSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flash Sale Repository
 * Sprint 16: Flash Sales System
 */
@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {
    
    /**
     * Find all flash sales by status
     */
    List<FlashSale> findByStatus(FlashSaleStatus status);
    
    /**
     * Find active flash sales
     */
    @Query("SELECT f FROM FlashSale f WHERE f.status = 'ACTIVE' " +
           "AND f.startTime <= :now AND f.endTime >= :now")
    List<FlashSale> findActiveFlashSales(LocalDateTime now);
    
    /**
     * Find upcoming flash sales
     */
    @Query("SELECT f FROM FlashSale f WHERE f.status = 'SCHEDULED' " +
           "AND f.startTime > :now ORDER BY f.startTime ASC")
    List<FlashSale> findUpcomingFlashSales(LocalDateTime now);
    
    /**
     * Find flash sales within date range
     */
    @Query("SELECT f FROM FlashSale f WHERE f.startTime >= :start AND f.endTime <= :end")
    List<FlashSale> findByDateRange(LocalDateTime start, LocalDateTime end);
}
