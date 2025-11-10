package com.tiki.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Order Statistics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {
    private Long shopId;
    private Integer todayOrders;
    private BigDecimal todayRevenue;
    private Integer pendingOrders;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
}
