package com.tiki.auth.dto;

import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Verify OTP Request DTO
 * Sprint 10 - Phone OTP Verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;
    
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP code must contain only digits")
    private String otpCode;
    
    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}
