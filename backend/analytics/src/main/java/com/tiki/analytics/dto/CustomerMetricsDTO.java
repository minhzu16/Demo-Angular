package com.tiki.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Customer Metrics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMetricsDTO {
    
    private Integer totalCustomers;
    private Integer newCustomers;
    private Integer returningCustomers;
    private Double retentionRate;
    private BigDecimal averageLifetimeValue;
    private Double repeatPurchaseRate;
    private Integer averageOrdersPerCustomer;
    private Integer period; // in days
}
