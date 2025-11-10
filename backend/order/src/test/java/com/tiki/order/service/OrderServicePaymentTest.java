package com.tiki.order.service;

import com.tiki.order.dto.PaymentInfoDTO;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OrderService payment methods
 * Sprint 10 - COD Payment
 */
@ExtendWith(MockitoExtension.class)
class OrderServicePaymentTest {

    @InjectMocks
    private OrderService orderService;

    @Test
    void testGetPaymentInfo() {
        // Given
        Integer orderId = 1;

        // When
        PaymentInfoDTO paymentInfo = orderService.getPaymentInfo(orderId);

        // Then
        assertNotNull(paymentInfo);
        assertEquals(orderId, paymentInfo.getOrderId());
        assertEquals(PaymentMethod.COD, paymentInfo.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, paymentInfo.getPaymentStatus());
    }

    @Test
    void testConfirmCODPayment() {
        // Given
        Integer orderId = 1;

        // When
        PaymentInfoDTO paymentInfo = orderService.confirmCODPayment(orderId);

        // Then
        assertNotNull(paymentInfo);
        assertEquals(orderId, paymentInfo.getOrderId());
        assertEquals(PaymentMethod.COD, paymentInfo.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, paymentInfo.getPaymentStatus());
        assertNotNull(paymentInfo.getPaidAt());
        assertEquals("COD payment confirmed by seller", paymentInfo.getPaymentNote());
    }

    @Test
    void testValidatePaymentMethod_COD_Success() {
        // Should not throw exception
        assertDoesNotThrow(() -> 
            orderService.validatePaymentMethod(PaymentMethod.COD)
        );
    }

    @Test
    void testValidatePaymentMethod_Null_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> orderService.validatePaymentMethod(null)
        );
        
        assertEquals("Payment method is required", exception.getMessage());
    }

    @Test
    void testValidatePaymentMethod_VNPay_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> orderService.validatePaymentMethod(PaymentMethod.VNPAY)
        );
        
        assertTrue(exception.getMessage().contains("not supported yet"));
    }

    @Test
    void testValidatePaymentMethod_Momo_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> orderService.validatePaymentMethod(PaymentMethod.MOMO)
        );
        
        assertTrue(exception.getMessage().contains("not supported yet"));
    }

    @Test
    void testValidatePaymentMethod_BankTransfer_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> orderService.validatePaymentMethod(PaymentMethod.BANK_TRANSFER)
        );
        
        assertTrue(exception.getMessage().contains("not supported yet"));
    }
}
