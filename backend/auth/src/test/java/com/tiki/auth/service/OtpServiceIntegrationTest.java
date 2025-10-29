package com.tiki.auth.service;

import com.tiki.auth.entity.PhoneOtpEntity;
import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import com.tiki.auth.repository.PhoneOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OtpService
 * Sprint 10 - Phone OTP Verification
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OtpServiceIntegrationTest {

    @Autowired
    private OtpService otpService;

    @Autowired
    private PhoneOtpRepository otpRepository;

    private static final String TEST_PHONE = "0901234567";

    @BeforeEach
    void setUp() {
        // Clean up test data
        otpRepository.deleteAll();
    }

    @Test
    void testGenerateAndSendOtp_Success() {
        // When
        PhoneOtpEntity otp = otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // Then
        assertNotNull(otp);
        assertNotNull(otp.getId());
        assertEquals(TEST_PHONE, otp.getPhone());
        assertEquals(OtpPurpose.VERIFY, otp.getPurpose());
        assertEquals(6, otp.getOtpCode().length());
        assertFalse(otp.getVerified());
        assertEquals(0, otp.getAttempts());
        assertNotNull(otp.getExpiresAt());
    }

    @Test
    void testVerifyOtp_ValidCode_Success() {
        // Given
        PhoneOtpEntity otp = otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);
        String otpCode = otp.getOtpCode();

        // When
        boolean verified = otpService.verifyOtp(TEST_PHONE, otpCode, OtpPurpose.VERIFY);

        // Then
        assertTrue(verified);
    }

    @Test
    void testVerifyOtp_InvalidCode_Failure() {
        // Given
        otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // When
        boolean verified = otpService.verifyOtp(TEST_PHONE, "999999", OtpPurpose.VERIFY);

        // Then
        assertFalse(verified);
    }

    @Test
    void testVerifyOtp_WrongPurpose_Failure() {
        // Given
        PhoneOtpEntity otp = otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // When
        boolean verified = otpService.verifyOtp(TEST_PHONE, otp.getOtpCode(), OtpPurpose.LOGIN);

        // Then
        assertFalse(verified);
    }

    @Test
    void testRateLimit_ExceedsLimit_ThrowsException() {
        // Given - Send 3 OTPs (max allowed)
        otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);
        otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);
        otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // When & Then - 4th attempt should fail
        assertThrows(RuntimeException.class, () -> {
            otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);
        });
    }

    @Test
    void testValidatePhoneNumber_Null_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            otpService.generateAndSendOtp(null, OtpPurpose.VERIFY);
        });
    }

    @Test
    void testValidatePhoneNumber_Empty_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            otpService.generateAndSendOtp("", OtpPurpose.VERIFY);
        });
    }

    @Test
    void testValidatePhoneNumber_TooShort_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            otpService.generateAndSendOtp("123", OtpPurpose.VERIFY);
        });
    }

    @Test
    void testResendOtp_Success() {
        // Given
        PhoneOtpEntity firstOtp = otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // When
        PhoneOtpEntity secondOtp = otpService.resendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // Then
        assertNotNull(secondOtp);
        assertNotEquals(firstOtp.getId(), secondOtp.getId());
        assertEquals(TEST_PHONE, secondOtp.getPhone());
    }
}
