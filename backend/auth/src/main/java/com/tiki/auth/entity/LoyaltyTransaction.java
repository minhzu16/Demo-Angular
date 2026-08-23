package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track loyalty points history
 */
@Entity
@Table(name = "loyalty_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer pointsChange; // Positive for reward, negative for redemption

    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(nullable = false)
    private String reason; // e.g., "ORDER_REWARD", "VOUCHER_REDEMPTION", "CANCELLATION_REFUND"

    private String referenceId; // e.g., Order ID or Voucher ID

    @CreationTimestamp
    private LocalDateTime createdAt;
}
