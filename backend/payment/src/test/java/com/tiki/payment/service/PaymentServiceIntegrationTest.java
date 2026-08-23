package com.tiki.payment.service;

import com.tiki.payment.dto.CreatePaymentRequest;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.entity.PaymentEntity;
import com.tiki.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(PaymentService.class)
@TestPropertySource(properties = {
    "vnp.payUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
    "vnp.returnUrl=http://localhost:8080/return",
    "vnp.tmnCode=TMN01",
    "vnp.hashSecret=SECRET"
})
@Transactional
public class PaymentServiceIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.tiki.payment.entity")
    @EnableJpaRepositories(basePackages = "com.tiki.payment.repository")
    static class TestConfig {
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void testCreatePayment_Stripe() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(1001);
        request.setAmount(new BigDecimal("250000"));
        request.setCurrency("VND");
        request.setPaymentMethod("STRIPE");

        PaymentDto paymentDto = paymentService.createPayment(request);

        assertNotNull(paymentDto);
        assertEquals(1001, paymentDto.getOrderId());
        assertEquals(new BigDecimal("250000"), paymentDto.getAmount());
        assertEquals("PENDING", paymentDto.getPaymentStatus());
        assertNull(paymentDto.getRedirectUrl());

        // Verify it was persisted to H2 DB
        PaymentEntity entity = paymentRepository.findByOrderId(1001).orElse(null);
        assertNotNull(entity);
        assertEquals("STRIPE", entity.getPaymentMethod());
    }

    @Test
    void testCreatePayment_VnPay() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(1002);
        request.setAmount(new BigDecimal("500000"));
        request.setCurrency("VND");
        request.setPaymentMethod("VNPAY");

        PaymentDto paymentDto = paymentService.createPayment(request);

        assertNotNull(paymentDto);
        assertEquals(1002, paymentDto.getOrderId());
        assertEquals("PENDING", paymentDto.getPaymentStatus());
        assertNotNull(paymentDto.getRedirectUrl());
        assertTrue(paymentDto.getRedirectUrl().contains("vnp_TmnCode=TMN01"));
    }

    @Test
    void testGetPaymentInfoByOrderId() {
        PaymentEntity entity = PaymentEntity.builder()
                .orderId(1003)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .transactionId("txn-123")
                .build();
        paymentRepository.save(entity);

        PaymentDto paymentDto = paymentService.getPaymentInfoByOrderId(1003);
        assertNotNull(paymentDto);
        assertEquals("txn-123", paymentDto.getTransactionId());
        assertEquals("COD", paymentDto.getPaymentMethod());
    }

    @Test
    void testConfirmPaymentByIntentId() {
        PaymentEntity entity = PaymentEntity.builder()
                .orderId(1004)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .paymentMethod("STRIPE")
                .paymentStatus("PENDING")
                .paymentIntentId("intent-abc")
                .build();
        paymentRepository.save(entity);

        PaymentDto paymentDto = paymentService.confirmPaymentByIntentId("intent-abc", "SUCCESS");
        assertNotNull(paymentDto);
        assertEquals("SUCCESS", paymentDto.getPaymentStatus());

        PaymentEntity updated = paymentRepository.findByOrderId(1004).orElse(null);
        assertNotNull(updated);
        assertEquals("SUCCESS", updated.getPaymentStatus());
    }

    @Test
    void testUpdatePaymentStatusByOrderId() {
        PaymentEntity entity = PaymentEntity.builder()
                .orderId(1005)
                .amount(new BigDecimal("150000"))
                .currency("VND")
                .paymentMethod("COD")
                .paymentStatus("PENDING")
                .build();
        paymentRepository.save(entity);

        PaymentDto paymentDto = paymentService.updatePaymentStatusByOrderId(1005, "SUCCESS");
        assertNotNull(paymentDto);
        assertEquals("SUCCESS", paymentDto.getPaymentStatus());
    }
}
