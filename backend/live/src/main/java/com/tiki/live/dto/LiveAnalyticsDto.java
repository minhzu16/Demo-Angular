package com.tiki.live.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveAnalyticsDto {
    private Long sessionId;
    private Integer totalViewers;
    private Integer peakViewers;
    private Integer totalLikes;
    private Integer totalShares;
    private Integer totalProductsSold;
    private BigDecimal estimatedRevenue;
}
