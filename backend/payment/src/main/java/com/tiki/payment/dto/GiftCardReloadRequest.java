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
public class GiftCardReloadRequest {
    @NotNull
    @DecimalMin(value = "10000.00", message = "Số tiền nạp tối thiểu là 10.000đ")
    private BigDecimal amount;
}
