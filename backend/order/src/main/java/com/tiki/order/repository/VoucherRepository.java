package com.tiki.order.repository;

import com.tiki.order.entity.VoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<VoucherEntity, Integer> {

    /**
     * Find voucher by code
     */
    Optional<VoucherEntity> findByCode(String code);

    /**
     * Find voucher by code (case insensitive)
     */
    Optional<VoucherEntity> findByCodeIgnoreCase(String code);

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);

    /**
     * Find all active vouchers
     */
    List<VoucherEntity> findByIsActiveTrue();

    /**
     * Find active vouchers that are currently valid (not expired)
     */
    @Query("SELECT v FROM VoucherEntity v WHERE v.isActive = true " +
           "AND v.startDate <= :now AND v.endDate > :now " +
           "AND v.usedCount < v.maxUsage")
    List<VoucherEntity> findActiveAndValidVouchers(@Param("now") LocalDateTime now);

    /**
     * Find vouchers by type
     */
    List<VoucherEntity> findByType(VoucherEntity.DiscountType type);

    /**
     * Find vouchers that are about to expire (within days)
     */
    @Query("SELECT v FROM VoucherEntity v WHERE v.isActive = true " +
           "AND v.endDate BETWEEN :now AND :expiryDate")
    List<VoucherEntity> findVouchersExpiringBetween(
            @Param("now") LocalDateTime now,
            @Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Find vouchers with low remaining usage
     */
    @Query("SELECT v FROM VoucherEntity v WHERE v.isActive = true " +
           "AND (v.maxUsage - v.usedCount) <= :threshold " +
           "AND v.endDate > :now")
    List<VoucherEntity> findVouchersWithLowUsage(
            @Param("threshold") Integer threshold,
            @Param("now") LocalDateTime now);

    /**
     * Find expired vouchers
     */
    @Query("SELECT v FROM VoucherEntity v WHERE v.endDate < :now")
    List<VoucherEntity> findExpiredVouchers(@Param("now") LocalDateTime now);

    /**
     * Count active vouchers
     */
    long countByIsActiveTrue();

    /**
     * Find vouchers by code pattern (for search)
     */
    @Query("SELECT v FROM VoucherEntity v WHERE LOWER(v.code) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<VoucherEntity> searchByCodePattern(@Param("pattern") String pattern);
}
