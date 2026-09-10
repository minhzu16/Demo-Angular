package com.tiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class TwoFactorDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetupResponse {
        private String secretKey;
        private String qrCodeUrl;
        private String totpUri;
        private String manualEntryKey;
        private List<String> backupCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        @NotBlank(message = "Verification code is required")
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private boolean enabled;
        private LocalDateTime enabledAt;
        private LocalDateTime lastUsedAt;
        private int remainingBackupCodes;
    }
}
