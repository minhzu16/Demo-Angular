package com.tiki.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogDto {
    private Long id;
    private Long adminId;
    private String adminEmail;
    private String action;
    private String targetType;
    private String targetId;
    private String detailsJson;
    private String ipAddress;
    private LocalDateTime createdAt;
}
