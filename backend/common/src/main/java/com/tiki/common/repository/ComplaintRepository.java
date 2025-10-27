package com.tiki.common.repository;

import com.tiki.common.entity.ComplaintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
    List<ComplaintEntity> findByBuyerId(Long buyerId);
    List<ComplaintEntity> findBySellerId(Long sellerId);
}
