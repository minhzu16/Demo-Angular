package com.tiki.order.entity;

import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
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
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", unique = true, length = 50)
    private String orderNumber;

    // Order Status (SPRINT 4)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Pricing breakdown (SPRINT 2 - Voucher)
    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "voucher_discount", precision = 10, scale = 2)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    // Shipping info (SPRINT 3)
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "shipping_province", length = 100)
    private String shippingProvince;

    @Column(name = "shipping_district", length = 100)
    private String shippingDistrict;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    private String shippingNote;

    // Payment info (SPRINT 10 - COD Payment)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Generate order number
     */
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    /**
     * Calculate total amount
     */
    public void calculateTotal() {
        this.totalAmount = subtotal
                .subtract(voucherDiscount != null ? voucherDiscount : BigDecimal.ZERO)
                .add(shippingFee != null ? shippingFee : BigDecimal.ZERO);
    }

    /**
     * Apply voucher discount
     */
    public void applyVoucher(String code, BigDecimal discount) {
        this.voucherCode = code;
        this.voucherDiscount = discount;
        calculateTotal();
    }

    /**
     * Mark payment as paid (for COD confirmation)
     * Sprint 10 - COD Payment
     */
    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Check if order is COD
     */
    public boolean isCOD() {
        return this.paymentMethod != null && this.paymentMethod.isCOD();
    }

    /**
     * Check if payment is completed
     */
    public boolean isPaymentCompleted() {
        return this.paymentStatus != null && this.paymentStatus.isCompleted();
    }

    /**
     * Order Status Enum
     */
    public enum OrderStatus {
        PENDING,          // Chờ xác nhận
        CONFIRMED,        // Đã xác nhận
        PROCESSING,       // Đang chuẩn bị hàng
        SHIPPING,         // Đang giao
        DELIVERED,        // Đã giao
        CANCELLED,        // Đã hủy
        RETURN_REQUESTED, // Yêu cầu trả hàng
        REFUNDED          // Đã hoàn tiền
    }
}


