package com.tiki.auth.controller;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.LoginRequest;
import com.tiki.auth.dto.RegisterRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Auth Controller - Handles authentication and user management
 * Refactored with Lombok for cleaner code
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        return authService.register(request);
    }
    
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("User login attempt: {}", request.getUsernameOrEmail());
        return authService.login(request);
    }
    
    @PostMapping("/refresh")
    public AuthResponse refreshToken(@RequestParam String refreshToken) {
        log.debug("Refreshing token");
        return authService.refreshToken(refreshToken);
    }
    
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestParam String refreshToken) {
        log.info("User logout");
        authService.logout(refreshToken);
    }
    
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        log.info("User logout from all devices");
        authService.logoutAll(authService.getCurrentUser().getId());
    }
    
    @GetMapping("/me")
    public AuthResponse.UserInfo getCurrentUser() {
        log.debug("Getting current user info");
        User user = authService.getCurrentUser();
        return new AuthResponse.UserInfo(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name()
        );
    }
}
