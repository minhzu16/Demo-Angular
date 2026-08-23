package com.tiki.order.service;

import com.tiki.order.dto.PaymentInfoDTO;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import com.tiki.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for OrderPaymentService payment methods
 * Sprint 10 - COD Payment
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @InjectMocks
    private OrderPaymentService OrderPaymentService;

    @Mock
    private OrderRepository orderRepository;

    private OrderEntity codPendingOrder;

    @BeforeEach
    void setUp() {
        codPendingOrder = new OrderEntity();
        codPendingOrder.setId(1);
        codPendingOrder.setUserId(1L);
        codPendingOrder.setSubtotal(BigDecimal.ZERO);
        codPendingOrder.setTotalAmount(BigDecimal.ZERO);
        codPendingOrder.setCustomerName("Test Customer");
        codPendingOrder.setCustomerPhone("0123456789");
        codPendingOrder.setShippingAddress("Test Address");
        // paymentMethod defaults to COD, paymentStatus defaults to PENDING
    }

    @Test
    void testGetPaymentInfo() {
        // Given
        Integer orderId = 1;

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(codPendingOrder));

        // When
        PaymentInfoDTO paymentInfo = OrderPaymentService.getPaymentInfo(orderId);

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

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(codPendingOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PaymentInfoDTO paymentInfo = OrderPaymentService.confirmCODPayment(orderId);

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
            OrderPaymentService.validatePaymentMethod(PaymentMethod.COD)
        );
    }

    @Test
    void testValidatePaymentMethod_Null_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> OrderPaymentService.validatePaymentMethod(null)
        );
        
        assertEquals("Phương thức thanh toán là bắt buộc", exception.getMessage());
    }

    @Test
    void testValidatePaymentMethod_VNPay_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> OrderPaymentService.validatePaymentMethod(PaymentMethod.VNPAY)
        );
        
        assertTrue(exception.getMessage().contains("chưa được hỗ trợ"));
    }

    @Test
    void testValidatePaymentMethod_Momo_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> OrderPaymentService.validatePaymentMethod(PaymentMethod.MOMO)
        );
        
        assertTrue(exception.getMessage().contains("chưa được hỗ trợ"));
    }

    @Test
    void testValidatePaymentMethod_BankTransfer_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> OrderPaymentService.validatePaymentMethod(PaymentMethod.BANK_TRANSFER)
        );
        
        assertTrue(exception.getMessage().contains("chưa được hỗ trợ"));
    }
}
