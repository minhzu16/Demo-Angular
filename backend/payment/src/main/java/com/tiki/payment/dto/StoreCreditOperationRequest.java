package com.tiki.payment.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class StoreCreditOperationRequest {

    @NotNull
    @DecimalMin("1000.00")
    private BigDecimal amount;

    private String type; // REFUND_CREDIT, ORDER_PAYMENT, ADMIN_ADJUSTMENT, etc.
    private String referenceId;
    private Integer orderId;
    private String note;
}
