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
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrderEntity> findAll();

    /**
     * Find orders by user ID
     * ✅ BUG 46 FIX: Use EntityGraph to fetch items eagerly and prevent N+1 queries
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrderEntity> findByUserId(Long userId);
    
    /**
     * Find orders by user ID and status
     * ✅ BUG 46 FIX: Use EntityGraph
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrderEntity> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    /**
     * Find orders by status
     * ✅ BUG 46 FIX: Use EntityGraph
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrderEntity> findByStatus(OrderStatus status);
    
    /**
     * Find orders by shop ID with pagination
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    org.springframework.data.domain.Page<OrderEntity> findByShopIdOrderByCreatedAtDesc(Long shopId, org.springframework.data.domain.Pageable pageable);

    /**
     * Find all orders by shop ID
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    List<OrderEntity> findByShopId(Long shopId);

    /**
     * Count today's orders for a specific shop
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.shopId = :shopId AND o.createdAt >= :startOfDay")
    Integer countTodayOrdersByShopId(@Param("shopId") Long shopId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Calculate today's revenue for a specific shop
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.shopId = :shopId AND o.createdAt >= :startOfDay AND o.status NOT IN ('CANCELLED', 'REFUNDED')")
    BigDecimal calculateTodayRevenueByShopId(@Param("shopId") Long shopId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Count pending orders for a specific shop
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.shopId = :shopId AND o.status = 'PENDING'")
    Integer countPendingOrdersByShopId(@Param("shopId") Long shopId);

    /**
     * Count total orders for a specific shop
     */
    Long countByShopId(Long shopId);

    /**
     * Calculate total revenue for a specific shop
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.shopId = :shopId AND o.status NOT IN ('CANCELLED', 'REFUNDED')")
    BigDecimal calculateTotalRevenueByShopId(@Param("shopId") Long shopId);

    /**
     * Count today's orders for a shop (platform fallback)
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :startOfDay")
    Integer countTodayOrders(@Param("startOfDay") LocalDateTime startOfDay);
    
    /**
     * Calculate today's revenue for a shop (platform fallback)
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.createdAt >= :startOfDay")
    BigDecimal calculateTodayRevenue(@Param("startOfDay") LocalDateTime startOfDay);
    
    /**
     * Count pending orders for a shop (platform fallback)
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = 'PENDING'")
    Integer countPendingOrders();
    
    /**
     * Find orders by order number
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"items"})
    OrderEntity findByOrderNumber(String orderNumber);
    
    /**
     * Get sold count for a product
     */
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status IN ('DELIVERED', 'CONFIRMED', 'PROCESSING', 'SHIPPING')")
    Integer getProductSoldCount();

    /**
     * Accurate real product sold count from OrderItemEntity
     */
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM OrderItemEntity i WHERE i.productId = :productId AND i.order.status IN ('DELIVERED', 'CONFIRMED', 'PROCESSING', 'SHIPPING')")
    Integer getProductSoldCountReal(@Param("productId") Long productId);

    /**
     * Get daily revenue stats within a range
     */
    @Query("SELECT CAST(o.createdAt AS date) as date, SUM(o.totalAmount) as revenue, COUNT(o) as ordCount " +
           "FROM OrderEntity o WHERE o.createdAt BETWEEN :start AND :end " +
           "GROUP BY CAST(o.createdAt AS date) ORDER BY date ASC")
    List<Object[]> getDailyRevenueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Fraud Detection Queries (Giai đoạn Q4)
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
    long countByCustomerPhoneAndCreatedAtAfter(String customerPhone, LocalDateTime after);
    long countByUserIdAndStatus(Long userId, OrderStatus status);
    List<OrderEntity> findByFraudRiskLevelInOrderByCreatedAtDesc(List<String> riskLevels);
}
