package com.tiki.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vouchers")
public class VoucherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "shop_id")
    private Long shopId;  // NULL = platform voucher, NOT NULL = shop-specific voucher

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DiscountType type;

    @Column(name = "value", precision = 10, scale = 2, nullable = false)
    private BigDecimal value;

    @Column(name = "min_order_value", precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "max_usage")
    private Integer maxUsage = 1000;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
    }

    /**
     * Check if voucher is valid
     */
    public boolean isValid(BigDecimal orderTotal) {
        LocalDateTime now = LocalDateTime.now();
        
        return isActive
                && now.isAfter(startDate)
                && now.isBefore(endDate)
                && orderTotal.compareTo(minOrderValue) >= 0
                && usedCount < maxUsage;
    }

    /**
     * Calculate discount amount
     */
    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!isValid(orderTotal)) {
            return BigDecimal.ZERO;
        }

        if (type == DiscountType.PERCENTAGE) {
            // Percentage discount: value is percentage (e.g., 10 for 10%)
            return orderTotal.multiply(value).divide(BigDecimal.valueOf(100));
        } else {
            // Fixed discount: value is fixed amount
            return value;
        }
    }

    /**
     * Increment used count
     */
    public void incrementUsedCount() {
        this.usedCount++;
    }

    /**
     * Check if voucher has reached max usage
     */
    public boolean hasReachedMaxUsage() {
        return usedCount >= maxUsage;
    }

    /**
     * Check if voucher is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    /**
     * Check if voucher has started
     */
    public boolean hasStarted() {
        return LocalDateTime.now().isAfter(startDate);
    }

    public enum DiscountType {
        PERCENTAGE,  // Giảm theo phần trăm (%)
        FIXED        // Giảm số tiền cố định (VND)
    }
}
