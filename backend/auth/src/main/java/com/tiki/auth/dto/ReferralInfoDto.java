package com.tiki.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralInfoDto {
    private String referralCode;
    private String referralLink;
    private Integer referralCount;
    private Integer totalPointsEarned;
    private Long referredBy;
}
