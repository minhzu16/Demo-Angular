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
@Table(name = "gift_cards", indexes = {
        @Index(name = "idx_gift_card_code", columnList = "code", unique = true),
        @Index(name = "idx_gift_card_buyer", columnList = "purchased_by_user_id"),
        @Index(name = "idx_gift_card_status_expiry", columnList = "status, expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardEntity {

    public enum GiftCardStatus {
        ACTIVE,
        REDEEMED,
        EXPIRED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code; // e.g. GIFT-ABCD-1234-EFGH

    @Column(name = "initial_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal initialBalance;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "purchased_by_user_id", nullable = false)
    private Long purchasedByUserId;

    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "personal_message", columnDefinition = "TEXT")
    private String personalMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private GiftCardStatus status = GiftCardStatus.ACTIVE;

    @Column(name = "is_reloadable")
    @Builder.Default
    private Boolean isReloadable = true;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
