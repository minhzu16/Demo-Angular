package com.tiki.common.repository;

import com.tiki.common.entity.ComplaintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
    List<ComplaintEntity> findByBuyerId(Long buyerId);
    List<ComplaintEntity> findBySellerId(Long sellerId);
    Page<ComplaintEntity> findBySellerId(Long sellerId, Pageable pageable);
    List<ComplaintEntity> findByOrderId(Long orderId);
    boolean existsByOrderIdAndStatus(Long orderId, ComplaintEntity.Status status);
    Page<ComplaintEntity> findByStatus(ComplaintEntity.Status status, Pageable pageable);
}
