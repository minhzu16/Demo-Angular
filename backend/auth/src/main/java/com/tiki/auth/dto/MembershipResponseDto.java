package com.tiki.auth.dto;

import com.tiki.auth.entity.MembershipEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponseDto {
    private Long id;
    private Long userId;
    private Long planId;
    private String planCode;
    private String planName;
    private MembershipEntity.MembershipStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;
    private Boolean autoRenew;
    private Integer freeShipUsedThisMonth;
    private Integer maxFreeShipPerMonth;
    private Double pointsMultiplier;
    private Boolean isMember;
}
