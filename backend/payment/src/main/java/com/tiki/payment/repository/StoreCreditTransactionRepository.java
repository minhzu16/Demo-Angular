package com.tiki.payment.repository;

import com.tiki.payment.entity.StoreCreditTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreCreditTransactionRepository extends JpaRepository<StoreCreditTransactionEntity, Long> {
    Page<StoreCreditTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
