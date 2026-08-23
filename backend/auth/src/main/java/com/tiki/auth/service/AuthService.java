package com.tiki.auth.service;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.LoginRequest;
import com.tiki.auth.dto.RegisterRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.exception.InvalidCredentialsException;
import com.tiki.auth.exception.UserAlreadyExistsException;
import com.tiki.auth.exception.UserNotFoundException;
import com.tiki.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());
        
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            request.setEmail(request.getUsername());
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.fromString(request.getRole()));
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("=== LOGIN ATTEMPT ===");
        log.info("Username/Email: {}", request.getUsernameOrEmail());
        log.info("Password length: {}", request.getPassword() != null ? request.getPassword().length() : 0);
        
        User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(), 
                request.getUsernameOrEmail()
        ).orElseThrow(() -> {
            log.error("LOGIN FAILED - User not found: {}", request.getUsernameOrEmail());
            return new UserNotFoundException("User not found");
        });

        // Load user roles from user_roles table
        userService.loadUserRoles(user);
        
        log.info("User found - Username: {}, Roles: {}", user.getUsername(), user.getRolesAsString());
        log.info("Password hash from DB: {}", user.getPasswordHash().substring(0, 20) + "...");
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Password matches: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.error("LOGIN FAILED - Invalid password for user: {}", user.getUsername());
            throw new InvalidCredentialsException("Invalid password");
        }

        log.info("LOGIN SUCCESS - User: {}, Roles: {}", user.getUsername(), user.getRolesAsString());
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        // ✅ BUG 27 FIX: Check if token was explicitly revoked (blacklisted in Redis)
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String storedUserId = redisTemplate.opsForValue().get(tokenKey);
        if (storedUserId == null) {
            log.warn("Refresh token not found in Redis (already logged out or expired)");
            throw new InvalidCredentialsException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, username)) {
            redisTemplate.delete(tokenKey); // clean up expired entry
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        // Rotate: revoke old token, issue new one
        redisTemplate.delete(tokenKey);
        String userTokensKey = USER_TOKENS_PREFIX + user.getId();
        redisTemplate.opsForSet().remove(userTokensKey, refreshToken);

        userService.loadUserRoles(user);
        return buildAuthResponse(user);
    }

    /**
     * Logout - Invalidate single refresh token
     * Sprint 14 - Implemented with Redis
     */
    public void logout(String refreshToken) {
        log.info("Logging out with refresh token");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        
        try {
            // Extract userId from token
            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Remove token from Redis
            String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
            redisTemplate.delete(tokenKey);
            
            // Remove token from user's token set
            String userTokensKey = USER_TOKENS_PREFIX + user.getId();
            redisTemplate.opsForSet().remove(userTokensKey, refreshToken);
            
            log.info("User {} logged out successfully", username);
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Logout all sessions - Invalidate all refresh tokens for user
     * Sprint 14 - Implemented with Redis
     */
    public void logoutAll(Long userId) {
        log.info("Logging out all sessions for user: {}", userId);
        
        try {
            // Get all tokens for user
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
            
            if (tokens != null && !tokens.isEmpty()) {
                // Delete all refresh tokens
                for (String token : tokens) {
                    String tokenKey = REFRESH_TOKEN_PREFIX + token;
                    redisTemplate.delete(tokenKey);
                }
                
                // Clear user's token set
                redisTemplate.delete(userTokensKey);
                
                log.info("Logged out {} sessions for user {}", tokens.size(), userId);
            } else {
                log.info("No active sessions found for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Error during logout all: {}", e.getMessage());
            throw new RuntimeException("Logout all failed: " + e.getMessage());
        }
    }

    /**
     * Get current authenticated user from Security Context
     * Sprint 14 - Implemented with SecurityContext
     */
    /**
     * Get current authenticated user from Security Context.
     *
     * ✅ BUG 26 FIX: Removed dangerous hardcoded fallback (user id=1).
     * If SecurityContext has no authenticated user, throw an exception — never silently
     * return a fake admin-level user that could bypass access control checks.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("No authenticated user found in SecurityContext");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }
    
    /**
     * Store refresh token in Redis
     * Sprint 14 - Helper method for session management
     */
    private void storeRefreshToken(Long userId, String refreshToken) {
        try {
            // Store token with expiry
            String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
            redisTemplate.opsForValue().set(tokenKey, userId.toString(), REFRESH_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);
            
            // Add token to user's token set
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().add(userTokensKey, refreshToken);
            redisTemplate.expire(userTokensKey, REFRESH_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);
            
            log.debug("Stored refresh token for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to store refresh token: {}", e.getMessage());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        // Use roles string for JWT token (e.g., "BUYER,SELLER")
        String rolesString = user.getRolesAsString();
        
        String accessToken = jwtService.generateToken(
                user.getId(), user.getUsername(), user.getEmail(), rolesString
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());
        
        // Store refresh token in Redis for session management
        storeRefreshToken(user.getId(), refreshToken);
        
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail(), rolesString);
        userInfo.setFullName(user.getFullName());
        userInfo.setAge(user.getAge());
        userInfo.setPhoneNumber(user.getPhoneNumber());
        userInfo.setAddress(user.getAddress());
        userInfo.setGender(user.getGender());
        userInfo.setWorkplace(user.getWorkplace());
        userInfo.setLoyaltyPoints(user.getLoyaltyPoints());
        userInfo.setLoyaltyTier(user.getLoyaltyTier() != null ? user.getLoyaltyTier().name() : "BRONZE");
        
        return new AuthResponse(
                accessToken, 
                refreshToken,
                userInfo
        );
    }
}
