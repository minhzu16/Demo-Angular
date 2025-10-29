package com.tiki.auth.dto;

import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Send OTP Request DTO
 * Sprint 10 - Phone OTP Verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;
    
    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}
