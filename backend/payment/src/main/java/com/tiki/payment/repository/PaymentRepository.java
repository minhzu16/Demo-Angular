package com.tiki.payment.repository;

import com.tiki.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(Integer orderId);
    Optional<PaymentEntity> findByPaymentIntentId(String paymentIntentId);
}
