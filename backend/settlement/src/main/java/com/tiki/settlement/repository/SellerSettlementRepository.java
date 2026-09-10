package com.tiki.settlement.repository;

import com.tiki.settlement.entity.SellerSettlementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerSettlementRepository extends JpaRepository<SellerSettlementEntity, Long> {

    Optional<SellerSettlementEntity> findByOrderId(Integer orderId);

    List<SellerSettlementEntity> findByShopIdAndSettlementPeriod(Long shopId, String settlementPeriod);

    Page<SellerSettlementEntity> findByShopId(Long shopId, Pageable pageable);

    @Query("SELECT s FROM SellerSettlementEntity s WHERE s.shopId = :shopId AND s.settlementPeriod = :period AND s.status = :status")
    List<SellerSettlementEntity> findByShopIdAndPeriodAndStatus(
            @Param("shopId") Long shopId,
            @Param("period") String period,
            @Param("status") SellerSettlementEntity.SettlementStatus status);

    boolean existsByOrderId(Integer orderId);
}
