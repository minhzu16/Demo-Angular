package com.tiki.auth.controller;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.LoginRequest;
import com.tiki.auth.dto.RegisterRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    
    @PostMapping("/refresh")
    public AuthResponse refreshToken(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
    
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestParam String refreshToken) {
        authService.logout(refreshToken);
    }
    
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        authService.logoutAll(authService.getCurrentUser().getId());
    }
    
    @GetMapping("/me")
    public AuthResponse.UserInfo getCurrentUser() {
        User user = authService.getCurrentUser();
        return new AuthResponse.UserInfo(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name()
        );
    }
}
