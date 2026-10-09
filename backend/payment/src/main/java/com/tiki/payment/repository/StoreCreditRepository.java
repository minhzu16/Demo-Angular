package com.tiki.payment.repository;

import com.tiki.payment.entity.StoreCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreCreditRepository extends JpaRepository<StoreCreditEntity, Long> {
    Optional<StoreCreditEntity> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM StoreCreditEntity s WHERE s.userId = :userId")
    Optional<StoreCreditEntity> findByUserIdForUpdate(@org.springframework.data.repository.query.Param("userId") Long userId);
}
