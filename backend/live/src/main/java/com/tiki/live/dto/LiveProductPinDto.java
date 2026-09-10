package com.tiki.live.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveProductPinDto {
    private Long id;
    private Long sessionId;
    private Long productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal livePrice;
    private Integer stockLimit;
    private Integer soldCount;
    private Boolean isCurrentlyPinned;
    private Integer displayOrder;
    private LocalDateTime pinnedAt;
}
