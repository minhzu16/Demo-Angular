package com.tiki.auth.service;

import com.tiki.auth.dto.LoginRequest;
import com.tiki.auth.dto.TwoFactorDtos;
import com.tiki.auth.entity.User;
import com.tiki.auth.exception.InvalidCredentialsException;
import com.tiki.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SetOperations<String, String> setOps;
    @Mock private UserService userService;
    @Mock private TwoFactorAuthService twoFactorAuthService;
    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setUsername("an");
        user.setPasswordHash("$2a$10$hash");
        user.setRole(User.Role.BUYER);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh");
        when(twoFactorAuthService.getStatus(7L)).thenReturn(TwoFactorDtos.StatusResponse.builder().enabled(false).build());
    }

    private LoginRequest request(String identity, String password, String code) {
        LoginRequest r = new LoginRequest();
        r.setUsernameOrEmail(identity);
        r.setPassword(password);
        r.setTwoFactorCode(code);
        return r;
    }

    @Test
    @DisplayName("Không có tài khoản và sai mật khẩu cho cùng một thông báo (không liệt kê được tài khoản)")
    void unknownUser_andWrongPassword_lookIdentical() {
        when(userRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameOrEmail("an", "an")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "$2a$10$hash")).thenReturn(false);

        InvalidCredentialsException a = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request("ghost", "x", null)));
        InvalidCredentialsException b = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request("an", "bad", null)));

        assertEquals(a.getMessage(), b.getMessage());
    }

    @Test
    @DisplayName("Đăng nhập đúng mật khẩu, chưa bật 2FA -> thành công")
    void correctPassword_without2fa_succeeds() {
        when(userRepository.findByUsernameOrEmail("an", "an")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "$2a$10$hash")).thenReturn(true);

        assertNotNull(authService.login(request("an", "good", null)));
    }

    @Test
    @DisplayName("Đã bật 2FA: thiếu mã hoặc sai mã -> từ chối; đúng mã -> thành công")
    void twoFactorEnabled_requiresValidCode() {
        when(userRepository.findByUsernameOrEmail("an", "an")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "$2a$10$hash")).thenReturn(true);
        when(twoFactorAuthService.getStatus(7L)).thenReturn(TwoFactorDtos.StatusResponse.builder().enabled(true).build());
        when(twoFactorAuthService.verify2FACode(7L, "111111")).thenReturn(false);
        when(twoFactorAuthService.verify2FACode(7L, "123456")).thenReturn(true);

        InvalidCredentialsException missing = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request("an", "good", null)));
        assertEquals(true, missing.getMessage().contains("2FA_REQUIRED"));
        assertThrows(InvalidCredentialsException.class, () -> authService.login(request("an", "good", "111111")));
        verify(jwtService, never()).generateToken(any(), any(), any(), any());

        assertNotNull(authService.login(request("an", "good", "123456")));
    }
}
