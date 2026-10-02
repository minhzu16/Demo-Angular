package com.tiki.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events", uniqueConstraints = {
    @UniqueConstraint(name = "uk_provider_txn", columnNames = {"provider", "providerTxnId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider; // e.g. "SEPAY", "VNPAY", "STRIPE"

    @Column(nullable = false, length = 100)
    private String providerTxnId; // Unique transaction reference from provider

    private Integer orderId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 30)
    private String status; // "COMPLETED", "AMOUNT_MISMATCH", "DUPLICATE", "FAILED"

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
