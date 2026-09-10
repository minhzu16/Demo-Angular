package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // e.g., TIKI_PRO_MONTHLY, TIKI_PRO_YEARLY

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_monthly", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", precision = 15, scale = 2)
    private BigDecimal priceYearly;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits; // JSON string of benefits

    @Column(name = "max_freeship_per_month")
    @Builder.Default
    private Integer maxFreeShipPerMonth = 10;

    @Column(name = "freeship_max_value", precision = 15, scale = 2)
    private BigDecimal freeShipMaxValue;

    @Column(name = "points_multiplier")
    @Builder.Default
    private Double pointsMultiplier = 2.0;

    @Column(name = "trial_days")
    @Builder.Default
    private Integer trialDays = 7;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
