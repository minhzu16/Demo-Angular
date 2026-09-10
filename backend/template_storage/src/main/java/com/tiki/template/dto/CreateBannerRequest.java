package com.tiki.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBannerRequest {
    private Long campaignId;

    @NotBlank(message = "Tiêu đề banner không được để trống")
    private String title;

    @NotBlank(message = "Đường dẫn ảnh banner không được để trống")
    private String imageUrl;

    private String targetUrl;
    private String position; // HOME_HERO, HOME_MIDDLE, CATEGORY_TOP, POPUP
    private Integer displayOrder;
}
