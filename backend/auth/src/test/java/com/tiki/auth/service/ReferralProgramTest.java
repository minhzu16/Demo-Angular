package com.tiki.auth.service;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.ReferralInfoDto;
import com.tiki.auth.dto.RegisterRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralProgramTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private User referrer;

    @BeforeEach
    void setUp() {
        referrer = new User();
        referrer.setId(10L);
        referrer.setUsername("top_referrer");
        referrer.setEmail("referrer@example.com");
        referrer.setReferralCode("REF_TOP123");
        referrer.setLoyaltyPoints(100);
        referrer.setReferralCount(2);
    }

    @Test
    @DisplayName("Referral - Đăng ký kèm mã giới thiệu hợp lệ nhận thưởng điểm cả 2 bên")
    void testRegisterWithReferralCode_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_buyer");
        request.setEmail("buyer@example.com");
        request.setPassword("Password123!");
        request.setRole("BUYER");
        request.setReferralCode("REF_TOP123");

        when(userRepository.existsByUsername("new_buyer")).thenReturn(false);
        when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");
        when(userRepository.findByReferralCode("REF_TOP123")).thenReturn(Optional.of(referrer));
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("mock_access_token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("mock_refresh_token");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(20L);
            }
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("new_buyer", response.getUser().getUsername());

        // Kiểm tra người giới thiệu được cộng 50 điểm và tăng số lượt giới thiệu
        assertEquals(150, referrer.getLoyaltyPoints());
        assertEquals(3, referrer.getReferralCount());
        verify(userRepository, times(2)).save(any(User.class)); // 1 for referrer, 1 for new user
    }

    @Test
    @DisplayName("Referral - Đăng ký không kèm mã vẫn hoạt động bình thường")
    void testRegisterWithoutReferralCode_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("normal_buyer");
        request.setEmail("normal@example.com");
        request.setPassword("Password123!");
        request.setRole("BUYER");

        when(userRepository.existsByUsername("normal_buyer")).thenReturn(false);
        when(userRepository.existsByEmail("normal@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("mock_access_token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("mock_refresh_token");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(21L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, never()).findByReferralCode(any());
    }

    @Test
    @DisplayName("Referral Info - Lấy thông tin referral của user chính xác")
    void testGetReferralInfo() {
        UserService serviceUnderTest = new UserService(userRepository, null, null, passwordEncoder);
        when(userRepository.findById(10L)).thenReturn(Optional.of(referrer));

        ReferralInfoDto info = serviceUnderTest.getReferralInfo(10L);

        assertNotNull(info);
        assertEquals("REF_TOP123", info.getReferralCode());
        assertTrue(info.getReferralLink().contains("REF_TOP123"));
        assertEquals(2, info.getReferralCount());
        assertEquals(100, info.getTotalPointsEarned()); // 2 * 50 = 100
    }
}
