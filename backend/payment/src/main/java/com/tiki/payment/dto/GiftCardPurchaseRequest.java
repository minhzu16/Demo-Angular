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
public class GiftCardPurchaseRequest {

    @NotNull(message = "Mệnh giá thẻ không được để trống")
    @DecimalMin(value = "10000.00", message = "Mệnh giá tối thiểu là 10.000đ")
    private BigDecimal amount;

    private String recipientEmail;
    private String recipientName;
    private String personalMessage;
    private Boolean isReloadable;
    private Integer validityDays; // default 365
}
