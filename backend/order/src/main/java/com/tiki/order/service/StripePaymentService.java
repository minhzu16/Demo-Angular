package com.tiki.order.service;

import com.tiki.order.dto.PaymentRequestDto;
import com.tiki.order.dto.PaymentResponseDto;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService {
    public PaymentResponseDto createPaymentIntent(PaymentRequestDto request) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setOrderId(request.getOrderId());
        dto.setStatus("PROCESSING");
        dto.setPaymentIntentId("pi_mock_" + request.getOrderId());
        dto.setClientSecret("secret_mock");
        return dto;
    }

    public PaymentResponseDto confirmPayment(String paymentIntentId) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentIntentId(paymentIntentId);
        dto.setStatus("SUCCESS");
        return dto;
    }

    public PaymentResponseDto cancelPayment(String paymentIntentId) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentIntentId(paymentIntentId);
        dto.setStatus("CANCELED");
        return dto;
    }

    public PaymentResponseDto getPayment(String paymentIntentId) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentIntentId(paymentIntentId);
        dto.setStatus("SUCCESS");
        return dto;
    }

    public void handleWebhook(String payload, String signature) {
        // no-op mock
    }
}


