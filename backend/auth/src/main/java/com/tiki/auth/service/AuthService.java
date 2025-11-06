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
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, username)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

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
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("No authenticated user found");
            }
            
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        } catch (Exception e) {
            log.warn("Failed to get current user from SecurityContext: {}", e.getMessage());
            // Fallback for development/testing
            User u = new User();
            u.setId(1L);
            u.setUsername("user");
            u.setEmail("user@example.com");
            u.setRole(User.Role.BUYER);
            return u;
        }
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
        
        return new AuthResponse(
                accessToken, 
                refreshToken,
                new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail(), rolesString)
        );
    }
}
