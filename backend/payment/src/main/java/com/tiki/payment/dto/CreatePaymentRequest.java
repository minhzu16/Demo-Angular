package com.tiki.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NotNull(message = "Order ID cannot be null")
    private Integer orderId;
    
    @NotNull(message = "Amount cannot be null")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency cannot be blank")
    private String currency; // "VND", "USD"
    
    @NotBlank(message = "Payment method cannot be blank")
    private String paymentMethod; // "STRIPE", "MOMO", "COD"
}
