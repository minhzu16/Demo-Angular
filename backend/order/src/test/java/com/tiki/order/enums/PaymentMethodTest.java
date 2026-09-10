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
    void testSepayIsOnlinePayment() {
        assertTrue(PaymentMethod.SEPAY.isOnlinePayment());
    }

    @Test
    void testCODIsCOD() {
        assertTrue(PaymentMethod.COD.isCOD());
    }

    @Test
    void testOtherMethodsAreNotCOD() {
        assertFalse(PaymentMethod.SEPAY.isCOD());
    }

    @Test
    void testDisplayNames() {
        assertEquals("Cash on Delivery", PaymentMethod.COD.getDisplayName());
        assertEquals("SePay", PaymentMethod.SEPAY.getDisplayName());
    }

    @Test
    void testAllValuesExist() {
        PaymentMethod[] methods = PaymentMethod.values();
        assertEquals(2, methods.length);
    }
}
