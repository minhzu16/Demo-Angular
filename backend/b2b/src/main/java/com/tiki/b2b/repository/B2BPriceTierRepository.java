package com.tiki.b2b.repository;

import com.tiki.b2b.entity.B2BPriceTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface B2BPriceTierRepository extends JpaRepository<B2BPriceTierEntity, Long> {
    List<B2BPriceTierEntity> findByProductIdAndIsActiveTrueOrderByMinQuantityAsc(Long productId);
}
