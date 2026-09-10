package com.tiki.auth.service;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.OAuthAccountDto;
import com.tiki.auth.dto.OAuthLoginRequest;
import com.tiki.auth.entity.OAuthAccount;
import com.tiki.auth.entity.User;
import com.tiki.auth.repository.OAuthAccountRepository;
import com.tiki.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse loginOrRegister(OAuthLoginRequest request) {
        log.info("Processing OAuth login: provider={}, providerUserId={}", request.getProvider(), request.getProviderUserId());

        Optional<OAuthAccount> existingAccount = oauthAccountRepository
                .findByProviderAndProviderUserId(request.getProvider(), request.getProviderUserId());

        if (existingAccount.isPresent()) {
            OAuthAccount oauthAccount = existingAccount.get();
            User user = userRepository.findById(oauthAccount.getUserId())
                    .orElseThrow(() -> new IllegalStateException("User associated with OAuth account not found: " + oauthAccount.getUserId()));

            if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
                oauthAccount.setAccessToken(request.getAccessToken());
                oauthAccountRepository.save(oauthAccount);
            }

            userService.loadUserRoles(user);
            log.info("Existing OAuth account authenticated: userId={}, provider={}", user.getId(), request.getProvider());
            return authService.buildAuthResponse(user);
        }

        // Check if an existing user matches the email
        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                linkAccount(user.getId(), request);
                userService.loadUserRoles(user);
                log.info("Linked new OAuth provider {} to existing user email: {}", request.getProvider(), email);
                return authService.buildAuthResponse(user);
            }
        }

        // Create a new user for this OAuth identity
        User newUser = createOAuthUser(request);
        linkAccount(newUser.getId(), request);
        userService.loadUserRoles(newUser);

        log.info("Created new user via OAuth: userId={}, username={}, provider={}", newUser.getId(), newUser.getUsername(), request.getProvider());
        return authService.buildAuthResponse(newUser);
    }

    @Transactional
    public OAuthAccountDto linkOAuth(Long userId, OAuthLoginRequest request) {
        log.info("Linking OAuth account for userId={}: provider={}", userId, request.getProvider());

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        Optional<OAuthAccount> existingOauth = oauthAccountRepository
                .findByProviderAndProviderUserId(request.getProvider(), request.getProviderUserId());
        if (existingOauth.isPresent()) {
            if (!existingOauth.get().getUserId().equals(userId)) {
                throw new IllegalStateException("Tài khoản mạng xã hội này đã được liên kết với người dùng khác");
            }
            return toDto(existingOauth.get());
        }

        OAuthAccount linked = linkAccount(userId, request);
        return toDto(linked);
    }

    public List<OAuthAccountDto> getLinkedAccounts(Long userId) {
        return oauthAccountRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void unlinkOAuth(Long userId, OAuthAccount.Provider provider) {
        log.info("Unlinking OAuth account for userId={}: provider={}", userId, provider);

        OAuthAccount account = oauthAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên kết OAuth với nhà cung cấp: " + provider));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<OAuthAccount> allAccounts = oauthAccountRepository.findByUserId(userId);
        boolean hasUsablePassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();

        if (!hasUsablePassword && allAccounts.size() <= 1) {
            throw new IllegalStateException("Không thể hủy liên kết phương thức đăng nhập duy nhất của tài khoản. Vui lòng thiết lập mật khẩu trước.");
        }

        oauthAccountRepository.delete(account);
        log.info("Successfully unlinked OAuth account for userId={}, provider={}", userId, provider);
    }

    private OAuthAccount linkAccount(Long userId, OAuthLoginRequest request) {
        OAuthAccount oauthAccount = new OAuthAccount();
        oauthAccount.setUserId(userId);
        oauthAccount.setProvider(request.getProvider());
        oauthAccount.setProviderUserId(request.getProviderUserId());
        oauthAccount.setEmail(request.getEmail());
        oauthAccount.setAccessToken(request.getAccessToken());
        oauthAccount.setCreatedAt(LocalDateTime.now());
        oauthAccount.setUpdatedAt(LocalDateTime.now());
        return oauthAccountRepository.save(oauthAccount);
    }

    private User createOAuthUser(OAuthLoginRequest request) {
        String baseUsername = generateBaseUsername(request);
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + counter++;
        }

        String email = request.getEmail();
        if (email == null || email.isBlank() || userRepository.existsByEmail(email)) {
            email = username + "@oauth.local";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.getName() != null && !request.getName().isBlank() ? request.getName() : username);
        // Secure random password hash for OAuth accounts (cannot be guessed)
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(User.Role.BUYER);
        user.setLoyaltyPoints(0);

        return userRepository.save(user);
    }

    private String generateBaseUsername(OAuthLoginRequest request) {
        if (request.getEmail() != null && request.getEmail().contains("@")) {
            String prefix = request.getEmail().substring(0, request.getEmail().indexOf('@'))
                    .replaceAll("[^a-zA-Z0-9_.]", "");
            if (!prefix.isBlank()) {
                return prefix;
            }
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            String sanitized = request.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
            if (!sanitized.isBlank()) {
                return sanitized;
            }
        }
        return request.getProvider().name().toLowerCase() + "_" + request.getProviderUserId().substring(0, Math.min(8, request.getProviderUserId().length()));
    }

    private OAuthAccountDto toDto(OAuthAccount account) {
        return OAuthAccountDto.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .provider(account.getProvider())
                .providerUserId(account.getProviderUserId())
                .email(account.getEmail())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
