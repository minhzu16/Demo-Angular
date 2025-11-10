package com.tiki.order.dto;

import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payment Information DTO
 * Sprint 10 - COD Payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoDTO {
    
    private Integer orderId;
    private String orderNumber;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private String paymentNote;
    
    /**
     * Check if payment can be confirmed
     */
    public boolean canConfirm() {
        return paymentStatus == PaymentStatus.PENDING && 
               paymentMethod == PaymentMethod.COD;
    }
}
