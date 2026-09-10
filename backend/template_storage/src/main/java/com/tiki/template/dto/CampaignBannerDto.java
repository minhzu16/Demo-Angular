package com.tiki.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignBannerDto {
    private Long id;
    private Long campaignId;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private String position;
    private Integer displayOrder;
    private Boolean active;
}
