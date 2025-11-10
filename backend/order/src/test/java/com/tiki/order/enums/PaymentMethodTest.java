package com.tiki.order.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentMethod enum
 * Sprint 10 - COD Payment
 */
class PaymentMethodTest {

    @Test
    void testCODIsNotOnlinePayment() {
        assertFalse(PaymentMethod.COD.isOnlinePayment());
    }

    @Test
    void testVNPayIsOnlinePayment() {
        assertTrue(PaymentMethod.VNPAY.isOnlinePayment());
    }

    @Test
    void testMomoIsOnlinePayment() {
        assertTrue(PaymentMethod.MOMO.isOnlinePayment());
    }

    @Test
    void testZaloPayIsOnlinePayment() {
        assertTrue(PaymentMethod.ZALOPAY.isOnlinePayment());
    }

    @Test
    void testBankTransferIsNotOnlinePayment() {
        assertFalse(PaymentMethod.BANK_TRANSFER.isOnlinePayment());
    }

    @Test
    void testCODIsCOD() {
        assertTrue(PaymentMethod.COD.isCOD());
    }

    @Test
    void testOtherMethodsAreNotCOD() {
        assertFalse(PaymentMethod.VNPAY.isCOD());
        assertFalse(PaymentMethod.MOMO.isCOD());
        assertFalse(PaymentMethod.ZALOPAY.isCOD());
        assertFalse(PaymentMethod.BANK_TRANSFER.isCOD());
    }

    @Test
    void testDisplayNames() {
        assertEquals("Cash on Delivery", PaymentMethod.COD.getDisplayName());
        assertEquals("Bank Transfer", PaymentMethod.BANK_TRANSFER.getDisplayName());
        assertEquals("VNPay", PaymentMethod.VNPAY.getDisplayName());
        assertEquals("Momo", PaymentMethod.MOMO.getDisplayName());
        assertEquals("ZaloPay", PaymentMethod.ZALOPAY.getDisplayName());
    }

    @Test
    void testAllValuesExist() {
        PaymentMethod[] methods = PaymentMethod.values();
        assertEquals(5, methods.length);
    }
}
