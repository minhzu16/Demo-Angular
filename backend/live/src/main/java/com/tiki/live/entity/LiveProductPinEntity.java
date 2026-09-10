package com.tiki.live.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "live_product_pins", indexes = {
        @Index(name = "idx_pin_session", columnList = "session_id, is_currently_pinned")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveProductPinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "live_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal livePrice;

    @Column(name = "stock_limit")
    private Integer stockLimit;

    @Column(name = "sold_count")
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "is_currently_pinned", nullable = false)
    @Builder.Default
    private Boolean isCurrentlyPinned = false;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @Column(name = "unpinned_at")
    private LocalDateTime unpinnedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
