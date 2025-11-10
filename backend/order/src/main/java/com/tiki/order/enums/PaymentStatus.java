package com.tiki.order.enums;

/**
 * Payment Status Enum
 * Sprint 10 - Day 1
 */
public enum PaymentStatus {
    /**
     * Payment is pending - Chờ thanh toán
     */
    PENDING("Pending"),
    
    /**
     * Payment completed successfully - Đã thanh toán
     */
    PAID("Paid"),
    
    /**
     * Payment failed - Thanh toán thất bại
     */
    FAILED("Failed"),
    
    /**
     * Payment refunded - Đã hoàn tiền
     */
    REFUNDED("Refunded"),
    
    /**
     * Payment cancelled - Đã hủy
     */
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if payment is completed
     */
    public boolean isCompleted() {
        return this == PAID;
    }
    
    /**
     * Check if payment can be refunded
     */
    public boolean canRefund() {
        return this == PAID;
    }
    
    /**
     * Check if payment is in final state
     */
    public boolean isFinalState() {
        return this == PAID || this == FAILED || this == REFUNDED || this == CANCELLED;
    }
}
