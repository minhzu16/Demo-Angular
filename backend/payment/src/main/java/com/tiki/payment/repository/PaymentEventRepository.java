package com.tiki.payment.repository;

import com.tiki.payment.entity.PaymentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, Long> {
    boolean existsByProviderAndProviderTxnId(String provider, String providerTxnId);
    Optional<PaymentEventEntity> findByProviderAndProviderTxnId(String provider, String providerTxnId);
}
