package com.tiki.auth.service;

import com.tiki.auth.entity.PhoneOtpEntity;
import com.tiki.auth.entity.PhoneOtpEntity.OtpPurpose;
import com.tiki.auth.repository.PhoneOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OtpService (business logic only)
 * Sprint 10 - Phone OTP Verification
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceIntegrationTest {

    @Mock
    private PhoneOtpRepository otpRepository;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private OtpService otpService;

    private static final String TEST_PHONE = "0901234567";

    private AtomicLong idSequence;

    @BeforeEach
    void setUp() {
        idSequence = new AtomicLong(1L);
    }

    @Test
    void testGenerateAndSendOtp_Success() {
        // Mặc định: không bị rate limit trong test này
        when(otpRepository.countRecentOtps(eq(TEST_PHONE), any(LocalDateTime.class)))
                .thenReturn(0L);

        // save gán ID tăng dần
        when(otpRepository.save(any(PhoneOtpEntity.class))).thenAnswer(invocation -> {
            PhoneOtpEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
            }
            return entity;
        });

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
        String otpCode = "123456";
        PhoneOtpEntity existing = PhoneOtpEntity.builder()
                .id(1L)
                .phone(TEST_PHONE)
                .otpCode(otpCode)
                .purpose(OtpPurpose.VERIFY)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .attempts(0)
                .build();

        when(otpRepository.findValidOtp(eq(TEST_PHONE), eq(OtpPurpose.VERIFY), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existing));

        // verifyOtp sẽ gọi save để lưu lại trạng thái
        when(otpRepository.save(any(PhoneOtpEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean verified = otpService.verifyOtp(TEST_PHONE, otpCode, OtpPurpose.VERIFY);

        // Then
        assertTrue(verified);
    }

    @Test
    void testVerifyOtp_InvalidCode_Failure() {
        // Given
        String realCode = "123456";
        PhoneOtpEntity existing = PhoneOtpEntity.builder()
                .id(1L)
                .phone(TEST_PHONE)
                .otpCode(realCode)
                .purpose(OtpPurpose.VERIFY)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .attempts(0)
                .build();

        when(otpRepository.findValidOtp(eq(TEST_PHONE), eq(OtpPurpose.VERIFY), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existing));

        // When
        boolean verified = otpService.verifyOtp(TEST_PHONE, "999999", OtpPurpose.VERIFY);

        // Then
        assertFalse(verified);
    }

    @Test
    void testVerifyOtp_WrongPurpose_Failure() {
        // Given
        String otpCode = "123456";

        // Với purpose LOGIN thì không có OTP hợp lệ
        when(otpRepository.findValidOtp(eq(TEST_PHONE), eq(OtpPurpose.LOGIN), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When
        boolean verified = otpService.verifyOtp(TEST_PHONE, otpCode, OtpPurpose.LOGIN);

        // Then
        assertFalse(verified);
    }

    @Test
    void testRateLimit_ExceedsLimit_ThrowsException() {
        // Given - Send 3 OTPs (max allowed)
        when(otpRepository.countRecentOtps(eq(TEST_PHONE), any(LocalDateTime.class)))
                .thenReturn(0L, 1L, 2L, 3L);

        when(otpRepository.save(any(PhoneOtpEntity.class))).thenAnswer(invocation -> {
            PhoneOtpEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
            }
            return entity;
        });

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
        when(otpRepository.countRecentOtps(eq(TEST_PHONE), any(LocalDateTime.class)))
                .thenReturn(0L, 0L);

        when(otpRepository.save(any(PhoneOtpEntity.class))).thenAnswer(invocation -> {
            PhoneOtpEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(idSequence.getAndIncrement());
            }
            return entity;
        });

        PhoneOtpEntity firstOtp = otpService.generateAndSendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // When
        PhoneOtpEntity secondOtp = otpService.resendOtp(TEST_PHONE, OtpPurpose.VERIFY);

        // Then
        assertNotNull(secondOtp);
        assertNotEquals(firstOtp.getId(), secondOtp.getId());
        assertEquals(TEST_PHONE, secondOtp.getPhone());
    }
}
