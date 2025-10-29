package com.tiki.auth.service;

import com.tiki.auth.entity.PhoneOtpEntity;
import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import com.tiki.auth.repository.PhoneOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OTP Service
 * Sprint 10 - Phone OTP Verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final PhoneOtpRepository otpRepository;
    private final SmsService smsService;
    
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_MINUTES = 10;
    private static final int MAX_OTP_PER_WINDOW = 3;
    
    /**
     * Generate and send OTP
     */
    @Transactional
    public PhoneOtpEntity generateAndSendOtp(String phone, OtpPurpose purpose) {
        // Validate phone number
        validatePhoneNumber(phone);
        
        // Check rate limiting
        checkRateLimit(phone);
        
        // Generate OTP code
        String otpCode = generateOtpCode();
        
        // Create OTP entity
        PhoneOtpEntity otp = PhoneOtpEntity.builder()
                .phone(phone)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .attempts(0)
                .build();
        
        // Save to database
        otp = otpRepository.save(otp);
        
        // Send SMS
        try {
            smsService.sendOtp(phone, otpCode, purpose);
            log.info("OTP sent to phone: {} for purpose: {}", maskPhone(phone), purpose);
        } catch (Exception e) {
            log.error("Failed to send OTP to phone: {}", maskPhone(phone), e);
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }
        
        return otp;
    }
    
    /**
     * Verify OTP
     */
    @Transactional
    public boolean verifyOtp(String phone, String otpCode, OtpPurpose purpose) {
        // Find valid OTP
        Optional<PhoneOtpEntity> otpOpt = otpRepository.findValidOtp(
            phone, 
            purpose, 
            LocalDateTime.now()
        );
        
        if (otpOpt.isEmpty()) {
            log.warn("No valid OTP found for phone: {}", maskPhone(phone));
            return false;
        }
        
        PhoneOtpEntity otp = otpOpt.get();
        
        // Check if can verify
        if (!otp.canVerify()) {
            log.warn("OTP cannot be verified for phone: {}", maskPhone(phone));
            return false;
        }
        
        // Increment attempts
        otp.incrementAttempts();
        
        // Verify code
        if (otp.getOtpCode().equals(otpCode)) {
            otp.markAsVerified();
            otpRepository.save(otp);
            log.info("OTP verified successfully for phone: {}", maskPhone(phone));
            return true;
        }
        
        // Save failed attempt
        otpRepository.save(otp);
        log.warn("Invalid OTP code for phone: {}", maskPhone(phone));
        return false;
    }
    
    /**
     * Resend OTP
     */
    @Transactional
    public PhoneOtpEntity resendOtp(String phone, OtpPurpose purpose) {
        log.info("Resending OTP to phone: {}", maskPhone(phone));
        return generateAndSendOtp(phone, purpose);
    }
    
    /**
     * Generate random OTP code
     */
    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int otp = random.nextInt(900000) + 100000; // 6-digit number
        return String.valueOf(otp);
    }
    
    /**
     * Validate phone number format
     */
    private void validatePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        
        // Remove spaces and special characters
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        
        // Check length (Vietnam phone: 10 digits)
        if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
    
    /**
     * Check rate limiting
     */
    private void checkRateLimit(String phone) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        Long recentCount = otpRepository.countRecentOtps(phone, since);
        
        if (recentCount >= MAX_OTP_PER_WINDOW) {
            throw new RuntimeException(
                String.format("Too many OTP requests. Please try again after %d minutes.", 
                    RATE_LIMIT_MINUTES)
            );
        }
    }
    
    /**
     * Mask phone number for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
    
    /**
     * Cleanup expired OTPs (scheduled task)
     */
    @Transactional
    public void cleanupExpiredOtps() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        otpRepository.deleteByExpiresAtBefore(cutoff);
        log.info("Cleaned up expired OTPs before: {}", cutoff);
    }
}
