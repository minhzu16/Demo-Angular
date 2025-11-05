package com.tiki.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Revenue Analytics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDTO {
    
    private LocalDate date;
    private BigDecimal revenue;
    private Integer orderCount;
    private BigDecimal averageOrderValue;
    private BigDecimal previousRevenue;
    private Double growthRate;
    
    /**
     * Calculate growth rate from previous period
     */
    public void calculateGrowthRate() {
        if (previousRevenue != null && previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = revenue.subtract(previousRevenue);
            this.growthRate = diff.divide(previousRevenue, 4, BigDecimal.ROUND_HALF_UP)
                                 .multiply(new BigDecimal("100"))
                                 .doubleValue();
        } else {
            this.growthRate = 0.0;
        }
    }
}
