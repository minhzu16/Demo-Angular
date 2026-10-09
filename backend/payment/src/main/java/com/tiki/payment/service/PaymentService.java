package com.tiki.payment.service;

import com.tiki.payment.client.OrderClient;
import com.tiki.payment.dto.CreatePaymentRequest;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.entity.PaymentEntity;
import com.tiki.payment.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;

    @Value("${sepay.bank-name:MBBank}")
    private String sepayBankName;
    
    @Value("${sepay.account-number:0123456789}")
    private String sepayAccountNumber;

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for order ID: {}", request.getOrderId());
        
        // Find existing payment for the same order if any
        PaymentEntity existing = paymentRepository.findByOrderId(request.getOrderId()).orElse(null);
        if (existing != null) {
            log.warn("Payment already exists for order id: {}, id: {}", request.getOrderId(), existing.getId());
            return mapToDto(existing);
        }

        PaymentEntity entity = PaymentEntity.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PENDING")
                .transactionId(UUID.randomUUID().toString()) // Initial placeholder
                .build();
        
        // If Stripe, we might leave paymentIntentId empty until actual creation in Controller or here.
        
        PaymentEntity saved = paymentRepository.save(entity);
        PaymentDto dto = mapToDto(saved);

        if ("SEPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            String paymentUrl = generateSepayUrl(request.getOrderId(), request.getAmount());
            dto.setRedirectUrl(paymentUrl);
        } else if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            dto.setRedirectUrl(""); // No redirect for COD
        }

        return dto;
    }

    private String generateSepayUrl(Integer orderId, BigDecimal amount) {
        // Generate VietQR format URL via SePay
        String memo = "DH" + orderId;
        return String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s", 
                             sepayAccountNumber, sepayBankName, amount.intValue(), memo);
    }
    
    @Transactional(readOnly = true)
    public PaymentDto getPaymentInfoByOrderId(Integer orderId) {
        log.info("Fetching payment info for order id: {}", orderId);
        PaymentEntity existing = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));
        return mapToDto(existing);
    }
    
    @Transactional
    public PaymentDto confirmPaymentByIntentId(String intentId, String status) {
        log.info("Confirming payment for intent: {}, status: {}", intentId, status);
        PaymentEntity existing = paymentRepository.findByPaymentIntentId(intentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent id: " + intentId));
        
        existing.setPaymentStatus(status);
        return mapToDto(paymentRepository.save(existing));
    }
    
    @Transactional
    public PaymentDto updatePaymentStatusByOrderId(Integer orderId, String status) {
        PaymentEntity existing = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));
        
        existing.setPaymentStatus(status);
        PaymentEntity saved = paymentRepository.save(existing);

        if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            try {
                if (orderClient != null) {
                    orderClient.updatePaymentStatus(orderId, "PAID", existing.getTransactionId());
                    log.info("Successfully notified OrderService that order {} is PAID", orderId);
                }
            } catch (Exception e) {
                log.error("Failed to notify OrderService for order {}: {}", orderId, e.getMessage());
            }
        }

        return mapToDto(saved);
    }

    @Transactional
    public PaymentDto refundPayment(Integer orderId) {
        log.info("Processing refund for order id: {}", orderId);
        PaymentEntity existing = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));

        String current = existing.getPaymentStatus();
        if (!"PAID".equalsIgnoreCase(current) && !"COMPLETED".equalsIgnoreCase(current)) {
            throw new IllegalStateException("Chỉ có thể hoàn tiền cho khoản đã thanh toán (hiện tại: " + current + ")");
        }

        existing.setPaymentStatus("REFUNDED");
        PaymentEntity saved = paymentRepository.save(existing);
        log.info("Payment for order {} marked as REFUNDED", orderId);
        return mapToDto(saved);
    }

    private PaymentDto mapToDto(PaymentEntity entity) {
        return PaymentDto.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .paymentStatus(entity.getPaymentStatus())
                .transactionId(entity.getTransactionId())
                .paymentIntentId(entity.getPaymentIntentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
