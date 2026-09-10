package com.tiki.order.enums;

/**
 * Payment Method Enum
 */
public enum PaymentMethod {
    /**
     * Cash on Delivery - Thanh toán khi nhận hàng
     */
    COD("Cash on Delivery"),
    
    /**
     * SePay - Chuyển khoản qua SePay QR
     */
    SEPAY("SePay");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if payment method requires online payment
     */
    public boolean isOnlinePayment() {
        return this == SEPAY;
    }
    
    /**
     * Check if payment method is COD
     */
    public boolean isCOD() {
        return this == COD;
    }
}
