package com.tiki.auth.controller;

import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.ChangePasswordRequest;
import com.tiki.auth.dto.UpdateProfileRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return ResponseEntity.ok(mapToUserInfo(user));
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuthResponse.UserInfo> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToUserInfo(user));
    }
    
    /**
     * Update current user's profile
     */
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse.UserInfo> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateProfile(request);
        return ResponseEntity.ok(mapToUserInfo(updatedUser));
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
    
    /**
     * Update user role (Server-to-Server API)
     */
    @PostMapping("/{id}/role")
    public ResponseEntity<Map<String, String>> updateRole(
            @PathVariable Long id,
            @RequestParam String role) {
        userService.updateUserRole(id, role);
        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
    }

    /**
     * Update user loyalty points (Server-to-Server API)
     */
    @PostMapping("/{id}/points")
    public ResponseEntity<Map<String, Object>> updatePoints(
            @PathVariable Long id, 
            @RequestParam int points) {
        User updatedUser = userService.updateLoyaltyPoints(id, points);
        return ResponseEntity.ok(Map.of(
            "id", updatedUser.getId(),
            "loyaltyPoints", updatedUser.getLoyaltyPoints()
        ));
    }
    
    /**
     * Get user loyalty points history
     */
    @GetMapping("/me/loyalty-history")
    public ResponseEntity<Page<Map<String, Object>>> getLoyaltyHistory(
            @RequestHeader(value = "X-User-Id") Long userId,
            Pageable pageable) {
        Page<com.tiki.auth.entity.LoyaltyTransaction> history = userService.getLoyaltyHistory(userId, pageable);
        
        Page<Map<String, Object>> result = history.map(t -> Map.of(
            "id", t.getId(),
            "pointsChange", t.getPointsChange(),
            "balanceAfter", t.getBalanceAfter(),
            "reason", t.getReason(),
            "referenceId", t.getReferenceId() != null ? t.getReferenceId() : "",
            "createdAt", t.getCreatedAt()
        ));
        
        return ResponseEntity.ok(result);
    }

    // Address management is handled by AddressController at /api/v1/users/addresses
    
    private AuthResponse.UserInfo mapToUserInfo(User user) {
        AuthResponse.UserInfo info = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
        info.setFullName(user.getFullName());
        info.setAge(user.getAge());
        info.setPhoneNumber(user.getPhoneNumber());
        info.setAddress(user.getAddress());
        info.setGender(user.getGender());
        info.setWorkplace(user.getWorkplace());
        info.setLoyaltyPoints(user.getLoyaltyPoints());
        info.setLoyaltyTier(user.getLoyaltyTier() != null ? user.getLoyaltyTier().name() : "BRONZE");
        return info;
    }
}
