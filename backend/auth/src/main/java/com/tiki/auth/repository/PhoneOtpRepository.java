package com.tiki.auth.repository;

import com.tiki.auth.entity.PhoneOtpEntity;
import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Phone OTP Repository
 * Sprint 10 - Phone OTP Verification
 */
@Repository
public interface PhoneOtpRepository extends JpaRepository<PhoneOtpEntity, Long> {
    
    /**
     * Find latest OTP by phone and purpose
     */
    Optional<PhoneOtpEntity> findFirstByPhoneAndPurposeOrderByCreatedAtDesc(
        String phone, 
        OtpPurpose purpose
    );
    
    /**
     * Find valid (not expired, not verified) OTP
     */
    @Query("SELECT o FROM PhoneOtpEntity o WHERE o.phone = :phone " +
           "AND o.purpose = :purpose AND o.verified = false " +
           "AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<PhoneOtpEntity> findValidOtp(
        @Param("phone") String phone,
        @Param("purpose") OtpPurpose purpose,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Count OTPs sent in time window (for rate limiting)
     */
    @Query("SELECT COUNT(o) FROM PhoneOtpEntity o WHERE o.phone = :phone " +
           "AND o.createdAt > :since")
    Long countRecentOtps(
        @Param("phone") String phone,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Find all OTPs for a phone
     */
    List<PhoneOtpEntity> findByPhoneOrderByCreatedAtDesc(String phone);
    
    /**
     * Delete expired OTPs (cleanup)
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
