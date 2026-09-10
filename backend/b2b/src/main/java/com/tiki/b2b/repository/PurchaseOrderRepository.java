package com.tiki.b2b.repository;

import com.tiki.b2b.entity.PurchaseOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    Optional<PurchaseOrderEntity> findByPoNumber(String poNumber);

    Page<PurchaseOrderEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
}
