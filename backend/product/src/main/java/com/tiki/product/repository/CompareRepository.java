package com.tiki.product.repository;

import com.tiki.product.entity.CompareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompareRepository extends JpaRepository<CompareEntity, Long> {

    List<CompareEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CompareEntity> findByUserIdAndProductId(Long userId, Integer productId);

    long countByUserId(Long userId);

    void deleteByUserIdAndProductId(Long userId, Integer productId);

    void deleteByUserId(Long userId);
}
