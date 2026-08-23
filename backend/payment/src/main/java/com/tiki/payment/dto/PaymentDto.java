package com.tiki.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long id;
    private Integer orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod; // STRIPE, MOMO, COD, ZALOPAY
    private String paymentStatus; // PENDING, SUCCESS, FAILED, CANCELLED
    private String transactionId;
    private String paymentIntentId; // Specifically for Stripe
    private String redirectUrl; // URL for VNPay/Momo redirect
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
