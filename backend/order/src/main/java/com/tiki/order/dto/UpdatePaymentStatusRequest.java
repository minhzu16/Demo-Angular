package com.tiki.order.dto;

import com.tiki.order.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating payment status from Payment Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequest {
    
    @NotNull
    private Integer orderId;
    
    @NotNull
    private PaymentStatus paymentStatus;
    
    private String transactionId;
    
    private String paymentMethod;
    
    private String gatewayResponse;
}
