package com.tiki.live.dto;

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
public class LiveSessionCreateRequest {

    private Long shopId;

    @NotBlank(message = "Tiêu đề phiên livestream không được để trống")
    private String title;

    private String description;
    private String thumbnailUrl;
    private LocalDateTime scheduledAt;
}
