package com.tiki.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rma_requests", indexes = {
        @Index(name = "idx_rma_order", columnList = "order_id"),
        @Index(name = "idx_rma_user_status", columnList = "user_id, status"),
        @Index(name = "idx_rma_shop_status", columnList = "shop_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmaEntity {

    public enum RmaType {
        REFUND,
        EXCHANGE,
        REPAIR
    }

    public enum ReasonCategory {
        DEFECTIVE,
        WRONG_ITEM,
        SIZE_MISMATCH,
        DAMAGED_IN_TRANSIT,
        CHANGED_MIND,
        OTHER
    }

    public enum RmaStatus {
        RMA_REQUESTED,
        RMA_APPROVED,
        RMA_REJECTED,
        ITEM_SHIPPED_BACK,
        ITEM_RECEIVED,
        INSPECTION_PASSED,
        INSPECTION_FAILED,
        REFUND_INITIATED,
        EXCHANGE_SHIPPED,
        RMA_DISPUTED,
        ADMIN_RESOLVED,
        COMPLETED,
        CANCELLED
    }

    public enum RefundMethod {
        ORIGINAL_PAYMENT,
        STORE_CREDIT,
        BANK_TRANSFER
    }

    public enum InspectionResult {
        PASSED,
        FAILED,
        PARTIAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rma_number", nullable = false, unique = true, length = 50)
    private String rmaNumber;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shop_id")
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private RmaType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 30)
    private ReasonCategory reasonCategory;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RmaStatus status = RmaStatus.RMA_REQUESTED;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "seller_note", columnDefinition = "TEXT")
    private String sellerNote;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "return_tracking_number", length = 100)
    private String returnTrackingNumber;

    @Column(name = "return_carrier", length = 100)
    private String returnCarrier;

    @Column(name = "exchange_order_id")
    private Integer exchangeOrderId;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", length = 30)
    @Builder.Default
    private RefundMethod refundMethod = RefundMethod.ORIGINAL_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_result", length = 30)
    private InspectionResult inspectionResult;

    @Column(name = "inspection_note", columnDefinition = "TEXT")
    private String inspectionNote;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
