package com.tiki.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OTP Response DTO
 * Sprint 10 - Phone OTP Verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    
    private boolean success;
    private String message;
    private String phone; // Masked
    private LocalDateTime expiresAt;
    private Integer remainingAttempts;
}
