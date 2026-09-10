package com.tiki.b2b.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "b2b_purchase_orders", indexes = {
        @Index(name = "idx_po_number", columnList = "po_number", unique = true),
        @Index(name = "idx_po_company_status", columnList = "company_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderEntity {

    public enum POStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        CONVERTED_TO_ORDER,
        REJECTED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber; // e.g. PO-20270501-XXXX

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy; // User ID of creator

    @Column(name = "approved_by")
    private Long approvedBy; // User ID of approver

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private POStatus status = POStatus.PENDING_APPROVAL;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount; // VAT 8%

    @Column(name = "grand_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "invoice_required")
    @Builder.Default
    private Boolean invoiceRequired = true;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "converted_order_id")
    private Integer convertedOrderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
