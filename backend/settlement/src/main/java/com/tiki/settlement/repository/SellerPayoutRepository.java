package com.tiki.settlement.repository;

import com.tiki.settlement.entity.SellerPayoutEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerPayoutRepository extends JpaRepository<SellerPayoutEntity, Long> {

    List<SellerPayoutEntity> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<SellerPayoutEntity> findByShopId(Long shopId, Pageable pageable);

    Optional<SellerPayoutEntity> findByShopIdAndSettlementPeriod(Long shopId, String settlementPeriod);

    List<SellerPayoutEntity> findByStatus(SellerPayoutEntity.PayoutStatus status);
}
