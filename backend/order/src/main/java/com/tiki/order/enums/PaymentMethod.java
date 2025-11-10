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
     * Bank Transfer - Chuyển khoản ngân hàng
     */
    BANK_TRANSFER("Bank Transfer"),
    
    /**
     * VNPay - Cổng thanh toán VNPay
     */
    VNPAY("VNPay"),
    
    /**
     * Momo - Ví điện tử Momo
     */
    MOMO("Momo"),
    
    /**
     * ZaloPay - Ví điện tử ZaloPay
     */
    ZALOPAY("ZaloPay");
    
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
        return this == VNPAY || this == MOMO || this == ZALOPAY;
    }
    
    /**
     * Check if payment method is COD
     */
    public boolean isCOD() {
        return this == COD;
    }
}
