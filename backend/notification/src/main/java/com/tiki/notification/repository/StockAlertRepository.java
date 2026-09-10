package com.tiki.notification.repository;

import com.tiki.notification.entity.StockAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlertEntity, Long> {

    List<StockAlertEntity> findByProductIdAndNotifiedFalse(Long productId);

    List<StockAlertEntity> findByUserIdAndNotifiedFalseOrderByCreatedAtDesc(Long userId);

    boolean existsByProductIdAndUserIdAndNotifiedFalse(Long productId, Long userId);
}
