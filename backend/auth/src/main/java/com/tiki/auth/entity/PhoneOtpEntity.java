package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phone OTP Entity
 * Sprint 10 - Phone OTP Verification
 */
@Entity
@Table(name = "phone_otps", indexes = {
    @Index(name = "idx_phone_purpose", columnList = "phone,purpose"),
    @Index(name = "idx_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOtpEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "phone", nullable = false, length = 15)
    private String phone;
    
    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private OtpPurpose purpose;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "verified")
    private Boolean verified = false;
    
    @Column(name = "attempts")
    private Integer attempts = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (verified == null) {
            verified = false;
        }
        if (attempts == null) {
            attempts = 0;
        }
    }
    
    /**
     * Check if OTP is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if OTP can be verified (not expired, not verified, attempts < 3)
     */
    public boolean canVerify() {
        return !isExpired() && !verified && attempts < 3;
    }
    
    /**
     * Increment verification attempts
     */
    public void incrementAttempts() {
        this.attempts++;
    }
    
    /**
     * Mark OTP as verified
     */
    public void markAsVerified() {
        this.verified = true;
    }
    
    /**
     * OTP Purpose Enum
     */
    public enum OtpPurpose {
        REGISTER,   // Registration verification
        LOGIN,      // Login verification
        VERIFY,     // Phone verification
        RESET       // Password reset
    }
}
