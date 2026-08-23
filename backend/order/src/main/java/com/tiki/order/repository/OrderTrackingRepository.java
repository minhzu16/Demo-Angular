package com.tiki.order.repository;

import com.tiki.order.entity.OrderTrackingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTrackingEntity, Long> {
    List<OrderTrackingEntity> findByOrderIdOrderByCreatedAtDesc(Integer orderId);
}
