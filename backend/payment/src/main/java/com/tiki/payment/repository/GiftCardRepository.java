package com.tiki.payment.repository;

import com.tiki.payment.entity.GiftCardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GiftCardRepository extends JpaRepository<GiftCardEntity, Long> {

    Optional<GiftCardEntity> findByCode(String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT g FROM GiftCardEntity g WHERE g.code = :code")
    Optional<GiftCardEntity> findByCodeForUpdate(@org.springframework.data.repository.query.Param("code") String code);

    Page<GiftCardEntity> findByPurchasedByUserIdOrderByCreatedAtDesc(Long purchasedByUserId, Pageable pageable);
}
