package com.tiki.payment.service;

import com.tiki.payment.dto.CreatePaymentRequest;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    public void testCreateSepayUrl() {
        // Set @Value fields manually
        ReflectionTestUtils.setField(paymentService, "sepayBankName", "MBBank");
        ReflectionTestUtils.setField(paymentService, "sepayAccountNumber", "0123456789");

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(123456);
        request.setAmount(new BigDecimal("1000000"));
        request.setCurrency("VND");
        request.setPaymentMethod("SEPAY");

        // Mock repository behavior
        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        PaymentDto response = paymentService.createPayment(request);
        
        assertNotNull(response);
        assertNotNull(response.getRedirectUrl());
        assertTrue(response.getRedirectUrl().contains("qr.sepay.vn/img"), "URL should be SePay QR URL");
        assertTrue(response.getRedirectUrl().contains("acc=0123456789"), "URL should contain account number");
    }

    @Test
    public void testRefundPayment() {
        com.tiki.payment.entity.PaymentEntity entity = com.tiki.payment.entity.PaymentEntity.builder()
                .id(1L)
                .orderId(123456)
                .amount(new BigDecimal("500000"))
                .paymentStatus("PAID")
                .build();

        when(paymentRepository.findByOrderId(123456)).thenReturn(Optional.of(entity));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        PaymentDto dto = paymentService.refundPayment(123456);

        assertNotNull(dto);
        assertEquals("REFUNDED", dto.getPaymentStatus());
        assertEquals(123456, dto.getOrderId());
    }
}
