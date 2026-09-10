package com.tiki.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipBenefitCheckDto {
    private boolean isMember;
    private String planName;
    private boolean freeShipping;
    private int freeShipRemaining;
    private BigDecimal freeShipMaxValue;
    private double pointsMultiplier;
    private boolean exclusiveDeals;
    private boolean prioritySupport;
}
