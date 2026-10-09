package com.tiki.auth.controller;

import com.tiki.auth.dto.OtpResponse;
import com.tiki.auth.dto.SendOtpRequest;
import com.tiki.auth.dto.VerifyOtpRequest;
import com.tiki.auth.entity.PhoneOtpEntity;
import com.tiki.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP Controller
 * Sprint 10 - Phone OTP Verification
 */
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpController {
    
    private final OtpService otpService;
    
    /**
     * Send OTP to phone
     * POST /api/v1/otp/send
     */
    @PostMapping("/send")
    public ResponseEntity<OtpResponse> sendOtp(@RequestBody(required = false) SendOtpRequest request) {
        // This endpoint used to answer "OTP sent (mock/fallback)" for a missing phone, an invalid phone, rate
        // limiting AND SMS failures — the user waited for a code that was never sent. Report what really happened.
        if (request == null || request.getPhone() == null || request.getPhone().isBlank()) {
            return failure(HttpStatus.BAD_REQUEST, "Vui lòng nhập số điện thoại");
        }
        try {
            PhoneOtpEntity otp = otpService.generateAndSendOtp(request.getPhone(), request.getPurpose());
            return ResponseEntity.ok(OtpResponse.builder()
                    .success(true)
                    .message("OTP sent successfully")
                    .phone(maskPhone(request.getPhone()))
                    .expiresAt(otp.getExpiresAt())
                    .remainingAttempts(3)
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid OTP request: {}", e.getMessage());
            return failure(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ");
        } catch (RuntimeException e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            boolean rateLimited = e.getMessage() != null && e.getMessage().startsWith("Too many OTP requests");
            return rateLimited
                    ? failure(HttpStatus.TOO_MANY_REQUESTS, "Bạn yêu cầu mã quá nhiều lần. Vui lòng thử lại sau ít phút.")
                    : failure(HttpStatus.BAD_GATEWAY, "Không gửi được tin nhắn OTP. Vui lòng thử lại.");
        }
    }

    private static ResponseEntity<OtpResponse> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(OtpResponse.builder().success(false).message(message).build());
    }
    
    /**
     * Verify OTP
     * POST /api/v1/otp/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            boolean verified = otpService.verifyOtp(
                request.getPhone(),
                request.getOtpCode(),
                request.getPurpose()
            );
            
            if (verified) {
                return ResponseEntity.ok(
                    OtpResponse.builder()
                        .success(true)
                        .message("OTP verified successfully")
                        .phone(maskPhone(request.getPhone()))
                        .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                    OtpResponse.builder()
                        .success(false)
                        .message("Invalid or expired OTP code")
                        .build()
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to verify OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                OtpResponse.builder()
                    .success(false)
                    .message("Failed to verify OTP")
                    .build()
            );
        }
    }
    
    /**
     * Resend OTP
     * POST /api/v1/otp/resend
     */
    @PostMapping("/resend")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            PhoneOtpEntity otp = otpService.resendOtp(
                request.getPhone(),
                request.getPurpose()
            );
            
            return ResponseEntity.ok(
                OtpResponse.builder()
                    .success(true)
                    .message("OTP resent successfully")
                    .phone(maskPhone(request.getPhone()))
                    .expiresAt(otp.getExpiresAt())
                    .remainingAttempts(3)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Failed to resend OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                OtpResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * Mask phone number for response
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
