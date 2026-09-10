package com.tiki.payment.repository;

import com.tiki.payment.entity.GiftCardTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftCardTransactionRepository extends JpaRepository<GiftCardTransactionEntity, Long> {
    List<GiftCardTransactionEntity> findByGiftCardIdOrderByCreatedAtDesc(Long giftCardId);
}
