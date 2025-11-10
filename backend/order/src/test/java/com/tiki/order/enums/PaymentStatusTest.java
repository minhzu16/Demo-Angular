package com.tiki.order.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentStatus enum
 * Sprint 10 - COD Payment
 */
class PaymentStatusTest {

    @Test
    void testPaidIsCompleted() {
        assertTrue(PaymentStatus.PAID.isCompleted());
    }

    @Test
    void testOtherStatusesAreNotCompleted() {
        assertFalse(PaymentStatus.PENDING.isCompleted());
        assertFalse(PaymentStatus.FAILED.isCompleted());
        assertFalse(PaymentStatus.REFUNDED.isCompleted());
        assertFalse(PaymentStatus.CANCELLED.isCompleted());
    }

    @Test
    void testPaidCanRefund() {
        assertTrue(PaymentStatus.PAID.canRefund());
    }

    @Test
    void testOtherStatusesCannotRefund() {
        assertFalse(PaymentStatus.PENDING.canRefund());
        assertFalse(PaymentStatus.FAILED.canRefund());
        assertFalse(PaymentStatus.REFUNDED.canRefund());
        assertFalse(PaymentStatus.CANCELLED.canRefund());
    }

    @Test
    void testFinalStates() {
        assertTrue(PaymentStatus.PAID.isFinalState());
        assertTrue(PaymentStatus.FAILED.isFinalState());
        assertTrue(PaymentStatus.REFUNDED.isFinalState());
        assertTrue(PaymentStatus.CANCELLED.isFinalState());
        assertFalse(PaymentStatus.PENDING.isFinalState());
    }

    @Test
    void testDisplayNames() {
        assertEquals("Pending", PaymentStatus.PENDING.getDisplayName());
        assertEquals("Paid", PaymentStatus.PAID.getDisplayName());
        assertEquals("Failed", PaymentStatus.FAILED.getDisplayName());
        assertEquals("Refunded", PaymentStatus.REFUNDED.getDisplayName());
        assertEquals("Cancelled", PaymentStatus.CANCELLED.getDisplayName());
    }

    @Test
    void testAllValuesExist() {
        PaymentStatus[] statuses = PaymentStatus.values();
        assertEquals(5, statuses.length);
    }
}
