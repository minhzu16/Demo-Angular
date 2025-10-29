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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

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

        log.info("User found - Username: {}, Role: {}", user.getUsername(), user.getRole());
        log.info("Password hash from DB: {}", user.getPasswordHash().substring(0, 20) + "...");
        
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Password matches: {}", passwordMatches);
        
        if (!passwordMatches) {
            log.error("LOGIN FAILED - Invalid password for user: {}", user.getUsername());
            throw new InvalidCredentialsException("Invalid password");
        }

        log.info("LOGIN SUCCESS - User: {}", user.getUsername());
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

    public void logout(String refreshToken) {
        // TODO: Invalidate refresh token in Redis
    }

    public void logoutAll(Long userId) {
        // TODO: Invalidate all refresh tokens for user in Redis
    }

    public User getCurrentUser() {
        // TODO: Get from Security Context
        User u = new User();
        u.setId(1L);
        u.setUsername("user");
        u.setEmail("user@example.com");
        u.setRole(User.Role.BUYER);
        return u;
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());
        
        return new AuthResponse(
                accessToken, 
                refreshToken,
                new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name())
        );
    }
}


