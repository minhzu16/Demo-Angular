package com.tiki.order.repository;

import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderEntity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    
    /**
     * Find orders by user ID
     */
    List<OrderEntity> findByUserId(Long userId);
    
    /**
     * Find orders by status
     */
    List<OrderEntity> findByStatus(OrderStatus status);
    
    /**
     * Count today's orders for a shop
     * Note: Shop ID needs to be added to OrderEntity or derived from order items
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :startOfDay")
    Integer countTodayOrders(@Param("startOfDay") LocalDateTime startOfDay);
    
    /**
     * Calculate today's revenue for a shop
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.createdAt >= :startOfDay")
    BigDecimal calculateTodayRevenue(@Param("startOfDay") LocalDateTime startOfDay);
    
    /**
     * Count pending orders for a shop
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = 'PENDING'")
    Integer countPendingOrders();
    
    /**
     * Find orders by order number
     */
    OrderEntity findByOrderNumber(String orderNumber);
    
    /**
     * Get sold count for a product
     * Sprint 14 - Product Search Service integration
     * Note: This is a placeholder. In real implementation, 
     * we need OrderItem table to track product quantities
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status IN ('DELIVERED', 'CONFIRMED', 'PROCESSING', 'SHIPPING')")
    Integer getProductSoldCount();
}
