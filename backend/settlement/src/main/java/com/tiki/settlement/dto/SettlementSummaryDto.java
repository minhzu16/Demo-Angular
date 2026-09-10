package com.tiki.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSummaryDto {
    private Long shopId;
    private String period;
    private int totalOrders;
    private BigDecimal totalOrderAmount;
    private BigDecimal totalCommissionAmount;
    private BigDecimal totalSellerPayoutAmount;
    private String payoutStatus;
}
