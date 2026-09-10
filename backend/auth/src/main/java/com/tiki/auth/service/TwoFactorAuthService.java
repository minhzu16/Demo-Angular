package com.tiki.auth.service;

import com.tiki.auth.dto.TwoFactorDtos;
import com.tiki.auth.entity.TwoFactorAuth;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.TwoFactorAuthRepository;
import com.tiki.auth.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final TwoFactorAuthRepository twoFactorRepository;
    private final UserRepository userRepository;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private static final String ISSUER = "Tiki Platform";
    private static final int BACKUP_CODE_COUNT = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public TwoFactorDtos.SetupResponse setup2FA(Long userId) {
        log.info("Initiating 2FA setup for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        GoogleAuthenticatorKey credentials = gAuth.createCredentials();
        String secretKey = credentials.getKey();
        List<String> backupCodes = generateBackupCodes();

        TwoFactorAuth twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseGet(() -> {
                    TwoFactorAuth entity = new TwoFactorAuth();
                    entity.setUserId(userId);
                    return entity;
                });

        twoFactor.setSecretKey(secretKey);
        twoFactor.setEnabled(false); // Only enable after user verifies first code
        twoFactor.setBackupCodes(String.join(",", backupCodes));
        twoFactorRepository.save(twoFactor);

        String accountName = user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : user.getUsername();
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(ISSUER, accountName, credentials);
        String totpUri = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(ISSUER, accountName, credentials);

        log.info("2FA credentials generated successfully for userId: {}", userId);
        return TwoFactorDtos.SetupResponse.builder()
                .secretKey(secretKey)
                .manualEntryKey(secretKey)
                .qrCodeUrl(qrCodeUrl)
                .totpUri(totpUri)
                .backupCodes(backupCodes)
                .build();
    }

    @Transactional
    public boolean enable2FA(Long userId, String code) {
        log.info("Enabling 2FA for userId: {}", userId);
        TwoFactorAuth twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Chưa thiết lập 2FA. Vui lòng gọi setup trước."));

        if (twoFactor.getSecretKey() == null || twoFactor.getSecretKey().isBlank()) {
            throw new IllegalStateException("Secret key 2FA không tồn tại. Vui lòng thiết lập lại.");
        }

        boolean valid = validateTotpCode(twoFactor.getSecretKey(), code);
        if (!valid) {
            log.warn("Invalid verification code during 2FA activation for userId: {}", userId);
            throw new IllegalArgumentException("Mã xác thực 2FA không chính xác hoặc đã hết hạn.");
        }

        twoFactor.setEnabled(true);
        twoFactor.setEnabledAt(LocalDateTime.now());
        twoFactor.setLastUsedAt(LocalDateTime.now());
        twoFactorRepository.save(twoFactor);

        log.info("2FA successfully enabled for userId: {}", userId);
        return true;
    }

    @Transactional
    public boolean disable2FA(Long userId, String code) {
        log.info("Disabling 2FA for userId: {}", userId);
        TwoFactorAuth twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa kích hoạt 2FA"));

        if (!Boolean.TRUE.equals(twoFactor.getEnabled())) {
            throw new IllegalStateException("2FA hiện đang không được kích hoạt");
        }

        boolean valid = verifyCodeInternal(twoFactor, code);
        if (!valid) {
            log.warn("Invalid code during 2FA deactivation for userId: {}", userId);
            throw new IllegalArgumentException("Mã xác thực không hợp lệ để tắt 2FA.");
        }

        twoFactor.setEnabled(false);
        twoFactor.setSecretKey("");
        twoFactor.setBackupCodes("");
        twoFactorRepository.save(twoFactor);

        log.info("2FA successfully disabled for userId: {}", userId);
        return true;
    }

    @Transactional
    public boolean verify2FACode(Long userId, String code) {
        TwoFactorAuth twoFactor = twoFactorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa kích hoạt 2FA"));

        if (!Boolean.TRUE.equals(twoFactor.getEnabled())) {
            return true; // Not enabled, pass-through
        }

        boolean valid = verifyCodeInternal(twoFactor, code);
        if (!valid) {
            log.warn("Failed 2FA verification for userId: {}", userId);
            return false;
        }

        twoFactor.setLastUsedAt(LocalDateTime.now());
        twoFactorRepository.save(twoFactor);
        log.info("2FA code verified successfully for userId: {}", userId);
        return true;
    }

    public TwoFactorDtos.StatusResponse getStatus(Long userId) {
        TwoFactorAuth twoFactor = twoFactorRepository.findByUserId(userId).orElse(null);
        if (twoFactor == null || !Boolean.TRUE.equals(twoFactor.getEnabled())) {
            return TwoFactorDtos.StatusResponse.builder()
                    .enabled(false)
                    .remainingBackupCodes(0)
                    .build();
        }

        int remaining = 0;
        if (twoFactor.getBackupCodes() != null && !twoFactor.getBackupCodes().isBlank()) {
            remaining = (int) Arrays.stream(twoFactor.getBackupCodes().split(","))
                    .filter(c -> !c.isBlank())
                    .count();
        }

        return TwoFactorDtos.StatusResponse.builder()
                .enabled(true)
                .enabledAt(twoFactor.getEnabledAt())
                .lastUsedAt(twoFactor.getLastUsedAt())
                .remainingBackupCodes(remaining)
                .build();
    }

    private boolean verifyCodeInternal(TwoFactorAuth twoFactor, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return false;
        }
        String cleanCode = rawCode.trim().replaceAll("\\s+", "");

        // 1. Check TOTP 6-digit numeric code
        if (cleanCode.matches("^\\d{6}$")) {
            if (validateTotpCode(twoFactor.getSecretKey(), cleanCode)) {
                return true;
            }
        }

        // 2. Check Backup Code (case-insensitive)
        if (twoFactor.getBackupCodes() != null && !twoFactor.getBackupCodes().isBlank()) {
            List<String> codes = new ArrayList<>(Arrays.asList(twoFactor.getBackupCodes().split(",")));
            String upperCode = cleanCode.toUpperCase();

            for (int i = 0; i < codes.size(); i++) {
                if (codes.get(i).trim().equalsIgnoreCase(upperCode)) {
                    // Consume used backup code
                    codes.remove(i);
                    twoFactor.setBackupCodes(String.join(",", codes));
                    log.info("Backup code consumed for userId: {}, remaining: {}", twoFactor.getUserId(), codes.size());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean validateTotpCode(String secretKey, String code) {
        try {
            int verificationCode = Integer.parseInt(code.trim());
            return gAuth.authorize(secretKey, verificationCode);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                if (j == 4) sb.append("-");
                sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
            }
            codes.add(sb.toString());
        }
        return codes;
    }
}
