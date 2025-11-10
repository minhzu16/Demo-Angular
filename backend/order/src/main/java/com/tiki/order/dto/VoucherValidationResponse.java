package com.tiki.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherValidationResponse {
    private Boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private VoucherDTO voucher;

    public static VoucherValidationResponse success(BigDecimal discountAmount, VoucherDTO voucher) {
        return new VoucherValidationResponse(true, "Voucher is valid", discountAmount, voucher);
    }

    public static VoucherValidationResponse error(String message) {
        return new VoucherValidationResponse(false, message, BigDecimal.ZERO, null);
    }
}
