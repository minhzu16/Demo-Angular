package com.tiki.payment.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class GiftCardApplyRequest {
    @NotBlank
    private String code;

    @NotNull
    @DecimalMin("1000.00")
    private BigDecimal amount;

    private Integer orderId;
}
