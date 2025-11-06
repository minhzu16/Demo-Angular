package com.tiki.auth.controller;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.ChangePasswordRequest;
import com.tiki.auth.dto.UpdateProfileRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        ));
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuthResponse.UserInfo> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        ));
    }
    
    /**
     * Update current user's profile
     */
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse.UserInfo> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateProfile(request);
        return ResponseEntity.ok(new AuthResponse.UserInfo(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getRole().name()
        ));
    }
    
    /**
     * Change current user's password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    /**
     * Delete current user's account
     */
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        userService.deleteAccount();
    }
}
