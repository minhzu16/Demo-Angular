package com.tiki.auth.service;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.OAuthAccountDto;
import com.tiki.auth.dto.OAuthLoginRequest;
import com.tiki.auth.entity.OAuthAccount;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.OAuthAccountRepository;
import com.tiki.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthService oauthService;

    private User sampleUser;
    private AuthResponse sampleAuthResponse;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@gmail.com");
        sampleUser.setPasswordHash("hashed_pwd");
        sampleUser.setRole(User.Role.BUYER);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(10L, "testuser", "test@gmail.com", "BUYER");
        sampleAuthResponse = new AuthResponse("mock-jwt-access-token", "mock-refresh-token", userInfo);
    }

    @Test
    @DisplayName("Login with existing OAuth account succeeds and issues JWT")
    void testLoginExistingOAuthAccount() {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(OAuthAccount.Provider.GOOGLE)
                .providerUserId("google-sub-12345")
                .email("test@gmail.com")
                .accessToken("token-xyz")
                .build();

        OAuthAccount account = new OAuthAccount();
        account.setId(1L);
        account.setUserId(10L);
        account.setProvider(OAuthAccount.Provider.GOOGLE);
        account.setProviderUserId("google-sub-12345");

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthAccount.Provider.GOOGLE, "google-sub-12345"))
                .thenReturn(Optional.of(account));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(authService.buildAuthResponse(sampleUser)).thenReturn(sampleAuthResponse);

        AuthResponse response = oauthService.loginOrRegister(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-jwt-access-token");
        verify(userService).loadUserRoles(sampleUser);
        verify(oauthAccountRepository).save(account);
    }

    @Test
    @DisplayName("Login with new OAuth matching existing email links account and issues JWT")
    void testLoginNewOAuthMatchesExistingEmail() {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(OAuthAccount.Provider.GOOGLE)
                .providerUserId("google-sub-999")
                .email("test@gmail.com")
                .name("Test User")
                .build();

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthAccount.Provider.GOOGLE, "google-sub-999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(oauthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.buildAuthResponse(sampleUser)).thenReturn(sampleAuthResponse);

        AuthResponse response = oauthService.loginOrRegister(request);

        assertThat(response).isNotNull();
        verify(oauthAccountRepository).save(argThat(acc -> 
                acc.getUserId().equals(10L) && 
                acc.getProvider() == OAuthAccount.Provider.GOOGLE && 
                acc.getProviderUserId().equals("google-sub-999")));
    }

    @Test
    @DisplayName("Login with new OAuth without existing email creates new User and links account")
    void testLoginNewOAuthCreatesUser() {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(OAuthAccount.Provider.FACEBOOK)
                .providerUserId("fb-sub-777")
                .email("newuser@facebook.com")
                .name("New FB User")
                .build();

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthAccount.Provider.FACEBOOK, "fb-sub-777"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@facebook.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-random-pw");

        User createdUser = new User();
        createdUser.setId(20L);
        createdUser.setUsername("newuser");
        createdUser.setEmail("newuser@facebook.com");
        when(userRepository.save(any(User.class))).thenReturn(createdUser);

        when(oauthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.buildAuthResponse(createdUser)).thenReturn(sampleAuthResponse);

        AuthResponse response = oauthService.loginOrRegister(request);

        assertThat(response).isNotNull();
        verify(userRepository).save(argThat(u -> u.getEmail().equals("newuser@facebook.com") && u.getRole() == User.Role.BUYER));
        verify(oauthAccountRepository).save(argThat(acc -> acc.getUserId().equals(20L)));
    }

    @Test
    @DisplayName("Link OAuth account to current user")
    void testLinkOAuthAccountSuccess() {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(OAuthAccount.Provider.GOOGLE)
                .providerUserId("google-sub-555")
                .email("test@gmail.com")
                .build();

        when(userRepository.existsById(10L)).thenReturn(true);
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthAccount.Provider.GOOGLE, "google-sub-555"))
                .thenReturn(Optional.empty());

        OAuthAccount savedAccount = new OAuthAccount();
        savedAccount.setId(5L);
        savedAccount.setUserId(10L);
        savedAccount.setProvider(OAuthAccount.Provider.GOOGLE);
        savedAccount.setProviderUserId("google-sub-555");
        when(oauthAccountRepository.save(any(OAuthAccount.class))).thenReturn(savedAccount);

        OAuthAccountDto dto = oauthService.linkOAuth(10L, request);

        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo(10L);
        assertThat(dto.getProvider()).isEqualTo(OAuthAccount.Provider.GOOGLE);
    }

    @Test
    @DisplayName("Link OAuth account fails if already linked to different user")
    void testLinkOAuthAccountConflict() {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(OAuthAccount.Provider.GOOGLE)
                .providerUserId("google-sub-555")
                .build();

        OAuthAccount existingOther = new OAuthAccount();
        existingOther.setUserId(99L); // different user
        existingOther.setProvider(OAuthAccount.Provider.GOOGLE);
        existingOther.setProviderUserId("google-sub-555");

        when(userRepository.existsById(10L)).thenReturn(true);
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthAccount.Provider.GOOGLE, "google-sub-555"))
                .thenReturn(Optional.of(existingOther));

        assertThatThrownBy(() -> oauthService.linkOAuth(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("người dùng khác");
    }

    @Test
    @DisplayName("Unlink OAuth account succeeds when user has a password")
    void testUnlinkOAuthSuccess() {
        OAuthAccount account = new OAuthAccount();
        account.setUserId(10L);
        account.setProvider(OAuthAccount.Provider.GOOGLE);

        when(oauthAccountRepository.findByUserIdAndProvider(10L, OAuthAccount.Provider.GOOGLE))
                .thenReturn(Optional.of(account));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(oauthAccountRepository.findByUserId(10L)).thenReturn(List.of(account));

        oauthService.unlinkOAuth(10L, OAuthAccount.Provider.GOOGLE);

        verify(oauthAccountRepository).delete(account);
    }
}
