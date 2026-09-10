package com.tiki.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignDto {
    private Long id;
    private String code;
    private String title;
    private String description;
    private String bannerImageUrl;
    private String landingPageUrl;
    private Integer discountPercentage;
    private String voucherCode;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean currentlyRunning;
}
