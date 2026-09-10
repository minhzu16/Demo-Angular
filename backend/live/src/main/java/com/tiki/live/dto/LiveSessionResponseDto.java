package com.tiki.live.dto;

import com.tiki.live.entity.LiveSessionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionResponseDto {
    private Long id;
    private Long shopId;
    private Long sellerId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String streamUrl;
    private String streamKey;
    private LiveSessionEntity.LiveStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer viewerCount;
    private Integer peakViewers;
    private Integer likeCount;
    private Integer shareCount;
    private LocalDateTime createdAt;
    private LiveProductPinDto currentlyPinnedProduct;
}
