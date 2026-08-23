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
    public void testCreateVnPayUrl() {
        // Set @Value fields manually
        ReflectionTestUtils.setField(paymentService, "vnp_TmnCode", "TMN01");
        ReflectionTestUtils.setField(paymentService, "vnp_PayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(paymentService, "vnp_HashSecret", "SECRET");
        ReflectionTestUtils.setField(paymentService, "vnp_ReturnUrl", "http://localhost:8080/return");

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(123456);
        request.setAmount(new BigDecimal("1000000"));
        request.setCurrency("VND");
        request.setPaymentMethod("VNPAY");

        // Mock repository behavior
        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        PaymentDto response = paymentService.createPayment(request);
        
        assertNotNull(response);
        assertNotNull(response.getRedirectUrl());
        assertTrue(response.getRedirectUrl().contains("vnp_TmnCode=TMN01"), "URL should contain Merchant Code");
        assertTrue(response.getRedirectUrl().contains("vnp_SecureHash"), "URL should contain Security Hash");
    }
}
