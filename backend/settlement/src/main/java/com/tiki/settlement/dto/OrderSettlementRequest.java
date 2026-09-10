package com.tiki.settlement.dto;

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
public class OrderSettlementRequest {
    @NotNull
    private Integer orderId;

    @NotNull
    private Long shopId;

    private Long categoryId;

    @NotNull
    private BigDecimal orderAmount;

    private String settlementPeriod; // e.g. "2027-01", optional
}
