package com.tiki.settlement.repository;

import com.tiki.settlement.entity.CommissionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionRuleRepository extends JpaRepository<CommissionRuleEntity, Long> {

    @Query("SELECT r FROM CommissionRuleEntity r WHERE r.isActive = true " +
           "AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :now) " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now) " +
           "AND r.categoryId = :categoryId ORDER BY r.createdAt DESC")
    List<CommissionRuleEntity> findActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM CommissionRuleEntity r WHERE r.isActive = true " +
           "AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= :now) " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now) " +
           "AND r.categoryId IS NULL ORDER BY r.createdAt DESC")
    List<CommissionRuleEntity> findActiveGlobalRules(@Param("now") LocalDateTime now);

    Optional<CommissionRuleEntity> findFirstByCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(Long categoryId);

    Optional<CommissionRuleEntity> findFirstByCategoryIdIsNullAndIsActiveTrueOrderByCreatedAtDesc();
}
