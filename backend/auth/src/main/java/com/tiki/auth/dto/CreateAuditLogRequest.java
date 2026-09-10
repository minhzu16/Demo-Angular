package com.tiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditLogRequest {
    @NotNull(message = "Admin ID không được để trống")
    private Long adminId;

    private String adminEmail;

    @NotBlank(message = "Action không được để trống")
    private String action;

    @NotBlank(message = "Target Type không được để trống")
    private String targetType;

    private String targetId;
    private String detailsJson;
    private String ipAddress;
}
