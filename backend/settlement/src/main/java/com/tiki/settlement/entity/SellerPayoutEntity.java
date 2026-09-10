package com.tiki.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_payouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPayoutEntity {

    public enum PayoutMethod {
        BANK_TRANSFER,
        MOMO,
        WALLET
    }

    public enum PayoutStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "settlement_period", nullable = false, length = 20)
    private String settlementPeriod;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "total_order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalOrderAmount;

    @Column(name = "total_commission", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCommission;

    @Column(name = "total_payout", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPayout;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", length = 30)
    @Builder.Default
    private PayoutMethod payoutMethod = PayoutMethod.BANK_TRANSFER;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
