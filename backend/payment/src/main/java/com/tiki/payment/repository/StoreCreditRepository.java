package com.tiki.payment.repository;

import com.tiki.payment.entity.StoreCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreCreditRepository extends JpaRepository<StoreCreditEntity, Long> {
    Optional<StoreCreditEntity> findByUserId(Long userId);
}
