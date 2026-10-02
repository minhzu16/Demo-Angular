package com.tiki.payment.repository;

import com.tiki.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(Integer orderId);
    Optional<PaymentEntity> findByPaymentIntentId(String paymentIntentId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE PaymentEntity p SET p.paymentStatus = :newStatus, p.transactionId = :txnId WHERE p.orderId = :orderId AND p.paymentStatus = 'PENDING'")
    int updatePaymentStatusConditional(
            @org.springframework.data.repository.query.Param("orderId") Integer orderId,
            @org.springframework.data.repository.query.Param("newStatus") String newStatus,
            @org.springframework.data.repository.query.Param("txnId") String txnId
    );
}
