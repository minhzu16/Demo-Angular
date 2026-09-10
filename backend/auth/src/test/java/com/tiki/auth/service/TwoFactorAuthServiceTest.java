package com.tiki.auth.service;

import com.tiki.auth.dto.TwoFactorDtos;
import com.tiki.auth.entity.TwoFactorAuth;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.TwoFactorAuthRepository;
import com.tiki.auth.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceTest {

    @Mock
    private TwoFactorAuthRepository twoFactorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TwoFactorAuthService twoFactorService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUsername("adminuser");
        sampleUser.setEmail("admin@tiki.vn");
    }

    @Test
    @DisplayName("setup2FA generates secret, 8 backup codes, and QR auth url")
    void testSetup2FA() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(twoFactorRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(twoFactorRepository.save(any(TwoFactorAuth.class))).thenAnswer(i -> i.getArgument(0));

        TwoFactorDtos.SetupResponse response = twoFactorService.setup2FA(10L);

        assertThat(response).isNotNull();
        assertThat(response.getSecretKey()).isNotBlank();
        assertThat(response.getQrCodeUrl()).contains("api.qrserver.com");
        assertThat(response.getTotpUri()).contains("otpauth://totp/");
        assertThat(response.getBackupCodes()).hasSize(8);

        verify(twoFactorRepository).save(argThat(entity -> 
                entity.getUserId().equals(10L) && 
                !entity.getEnabled() && 
                entity.getSecretKey().equals(response.getSecretKey())));
    }

    @Test
    @DisplayName("enable2FA fails with invalid code")
    void testEnable2FAWithInvalidCode() {
        TwoFactorAuth entity = new TwoFactorAuth();
        entity.setUserId(10L);
        entity.setSecretKey("TESTSECRETKEY12345");
        entity.setEnabled(false);

        when(twoFactorRepository.findByUserId(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> twoFactorService.enable2FA(10L, "000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không chính xác");

        assertThat(entity.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("enable2FA succeeds with valid TOTP code")
    void testEnable2FAWithValidCode() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        String secretKey = gAuth.createCredentials().getKey();
        int validCode = gAuth.getTotpPassword(secretKey);

        TwoFactorAuth entity = new TwoFactorAuth();
        entity.setUserId(10L);
        entity.setSecretKey(secretKey);
        entity.setEnabled(false);

        when(twoFactorRepository.findByUserId(10L)).thenReturn(Optional.of(entity));

        boolean result = twoFactorService.enable2FA(10L, String.format("%06d", validCode));

        assertThat(result).isTrue();
        assertThat(entity.getEnabled()).isTrue();
        assertThat(entity.getEnabledAt()).isNotNull();
        verify(twoFactorRepository).save(entity);
    }

    @Test
    @DisplayName("verify2FACode succeeds and consumes backup code")
    void testVerifyBackupCode() {
        TwoFactorAuth entity = new TwoFactorAuth();
        entity.setUserId(10L);
        entity.setSecretKey("TESTKEY");
        entity.setEnabled(true);
        entity.setBackupCodes("CODE-1111,CODE-2222,CODE-3333");

        when(twoFactorRepository.findByUserId(10L)).thenReturn(Optional.of(entity));

        boolean valid = twoFactorService.verify2FACode(10L, "code-2222");

        assertThat(valid).isTrue();
        assertThat(entity.getBackupCodes()).isEqualTo("CODE-1111,CODE-3333"); // CODE-2222 removed!
        verify(twoFactorRepository).save(entity);
    }

    @Test
    @DisplayName("disable2FA with backup code succeeds")
    void testDisable2FAWithBackupCode() {
        TwoFactorAuth entity = new TwoFactorAuth();
        entity.setUserId(10L);
        entity.setSecretKey("TESTKEY");
        entity.setEnabled(true);
        entity.setBackupCodes("BACKUP-01,BACKUP-02");

        when(twoFactorRepository.findByUserId(10L)).thenReturn(Optional.of(entity));

        boolean disabled = twoFactorService.disable2FA(10L, "BACKUP-01");

        assertThat(disabled).isTrue();
        assertThat(entity.getEnabled()).isFalse();
        assertThat(entity.getSecretKey()).isEmpty();
        verify(twoFactorRepository).save(entity);
    }

    @Test
    @DisplayName("getStatus accurately reports 2FA state and remaining backup codes")
    void testGetStatus() {
        TwoFactorAuth entity = new TwoFactorAuth();
        entity.setUserId(10L);
        entity.setEnabled(true);
        entity.setBackupCodes("C1,C2,C3,C4,C5");

        when(twoFactorRepository.findByUserId(10L)).thenReturn(Optional.of(entity));

        TwoFactorDtos.StatusResponse status = twoFactorService.getStatus(10L);

        assertThat(status.isEnabled()).isTrue();
        assertThat(status.getRemainingBackupCodes()).isEqualTo(5);
    }
}
