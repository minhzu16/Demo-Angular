package com.tiki.auth.service;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.RegisterRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        when(jwtService.generateToken(any(), any(), any(), any())).thenReturn("mock_access_token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("mock_refresh_token");
    }

    @Test
    @DisplayName("P0 Security (3.1): Cố tình gửi role=ADMIN khi đăng ký -> Bắt buộc bị gán BUYER role")
    void testRegister_PrivilegeEscalationAttempt_AlwaysAssignsBuyerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("attacker");
        request.setEmail("attacker@hacker.io");
        request.setPassword("Secret123!");
        request.setRole("ADMIN"); // Attack payload attempting to gain admin privileges

        when(userRepository.existsByUsername("attacker")).thenReturn(false);
        when(userRepository.existsByEmail("attacker@hacker.io")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(User.Role.BUYER, savedUser.getRole(), 
                "Lỗ hổng 3.1: Tài khoản đăng ký qua API công khai phải luôn nhận BUYER role, không thể nhận ADMIN!");
        assertNotEquals(User.Role.ADMIN, savedUser.getRole());
    }

    @Test
    @DisplayName("P0 Security (3.1): Cố tình gửi role=SELLER khi đăng ký -> Bắt buộc bị gán BUYER role")
    void testRegister_SellerRoleAttempt_AlwaysAssignsBuyerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("fake_seller");
        request.setEmail("seller@fake.io");
        request.setPassword("Secret123!");
        request.setRole("SELLER");

        when(userRepository.existsByUsername("fake_seller")).thenReturn(false);
        when(userRepository.existsByEmail("seller@fake.io")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(User.Role.BUYER, savedUser.getRole(),
                "Lên SELLER phải qua luồng nộp hồ sơ được duyệt, không được gán trực tiếp khi register");
    }

    @Test
    @DisplayName("P0 Security (3.1): Đăng ký thông thường không có role -> Nhận BUYER role chuẩn")
    void testRegister_NormalRegistration_AssignsBuyerRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("normal_buyer");
        request.setEmail("buyer@example.com");
        request.setPassword("Secret123!");
        request.setRole(null);

        when(userRepository.existsByUsername("normal_buyer")).thenReturn(false);
        when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(101L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(User.Role.BUYER, savedUser.getRole());
    }
}
