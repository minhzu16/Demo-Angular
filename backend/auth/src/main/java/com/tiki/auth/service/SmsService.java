package com.tiki.auth.service;

import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS Service (Mock Implementation)
 * Sprint 10 - Phone OTP Verification
 * 
 * TODO: Replace with real SMS provider (Twilio, AWS SNS, etc.)
 */
@Service
@Slf4j
public class SmsService {
    
    /**
     * Send OTP via SMS
     * Currently mocked - logs OTP instead of sending
     */
    public void sendOtp(String phone, String otpCode, OtpPurpose purpose) {
        // Mock implementation - just log
        String message = buildOtpMessage(otpCode, purpose);
        
        log.info("=".repeat(50));
        log.info("📱 SMS MOCK - Sending OTP");
        log.info("To: {}", phone);
        log.info("Message: {}", message);
        log.info("OTP Code: {}", otpCode);
        log.info("=".repeat(50));
        
        // TODO: Implement real SMS sending
        // Example with Twilio:
        // twilioClient.messages.create(
        //     new Message.Creator(
        //         new PhoneNumber(phone),
        //         new PhoneNumber(twilioPhoneNumber),
        //         message
        //     )
        // );
        
        // Simulate SMS sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Build OTP message based on purpose
     */
    private String buildOtpMessage(String otpCode, OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> String.format(
                "Your Tiki registration code is: %s. Valid for 5 minutes. Do not share this code.",
                otpCode
            );
            case LOGIN -> String.format(
                "Your Tiki login code is: %s. Valid for 5 minutes. Do not share this code.",
                otpCode
            );
            case VERIFY -> String.format(
                "Your Tiki phone verification code is: %s. Valid for 5 minutes.",
                otpCode
            );
            case RESET -> String.format(
                "Your Tiki password reset code is: %s. Valid for 5 minutes. Do not share this code.",
                otpCode
            );
        };
    }
    
    /**
     * Send generic SMS
     */
    public void sendSms(String phone, String message) {
        log.info("=".repeat(50));
        log.info("📱 SMS MOCK - Sending Message");
        log.info("To: {}", phone);
        log.info("Message: {}", message);
        log.info("=".repeat(50));
        
        // TODO: Implement real SMS sending
    }
}
