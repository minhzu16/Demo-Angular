package com.tiki.common.repository;

import com.tiki.common.entity.SellerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DEPRECATED: Not actively used.
 * Commented out to reduce unnecessary bean registration.
 */
public interface SellerRepository extends JpaRepository<SellerEntity, Long> {
    Optional<SellerEntity> findByUserId(Long userId);
}
