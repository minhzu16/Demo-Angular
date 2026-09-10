package com.tiki.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {
    @NotBlank(message = "Mã chiến dịch không được để trống")
    private String code;

    @NotBlank(message = "Tên chiến dịch không được để trống")
    private String title;

    private String description;
    private String bannerImageUrl;
    private String landingPageUrl;
    private Integer discountPercentage;
    private String voucherCode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
