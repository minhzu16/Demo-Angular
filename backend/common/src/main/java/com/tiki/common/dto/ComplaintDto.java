package com.tiki.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintDto {
    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private String title;
    private String description;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
