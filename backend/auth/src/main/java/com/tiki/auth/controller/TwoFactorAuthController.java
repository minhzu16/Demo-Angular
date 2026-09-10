package com.tiki.auth.controller;

import com.tiki.auth.dto.TwoFactorDtos;
import com.tiki.auth.service.TwoFactorAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorService;

    /**
     * Authenticated endpoint: Initialize 2FA setup, returns secret key, QR url, and backup codes
     */
    @PostMapping("/setup")
    public ResponseEntity<TwoFactorDtos.SetupResponse> setup2FA(@RequestHeader("X-User-Id") Long userId) {
        log.info("Received request to setup 2FA for userId: {}", userId);
        return ResponseEntity.ok(twoFactorService.setup2FA(userId));
    }

    /**
     * Authenticated endpoint: Enable 2FA after validating first TOTP code
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enable2FA(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TwoFactorDtos.VerifyRequest request) {
        log.info("Received request to enable 2FA for userId: {}", userId);
        twoFactorService.enable2FA(userId, request.getCode());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xác thực 2 lớp (2FA) đã được kích hoạt thành công."
        ));
    }

    /**
     * Authenticated endpoint: Disable 2FA with valid TOTP code or backup code
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disable2FA(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TwoFactorDtos.VerifyRequest request) {
        log.info("Received request to disable 2FA for userId: {}", userId);
        twoFactorService.disable2FA(userId, request.getCode());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xác thực 2 lớp (2FA) đã được tắt."
        ));
    }

    /**
     * Authenticated endpoint: Check 2FA status
     */
    @GetMapping("/status")
    public ResponseEntity<TwoFactorDtos.StatusResponse> getStatus(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(twoFactorService.getStatus(userId));
    }

    /**
     * Authenticated endpoint: Verify a 2FA code during login step 2 or sensitive actions
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TwoFactorDtos.VerifyRequest request) {
        boolean valid = twoFactorService.verify2FACode(userId, request.getCode());
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "message", valid ? "Mã xác thực hợp lệ" : "Mã xác thực không hợp lệ"
        ));
    }
}
