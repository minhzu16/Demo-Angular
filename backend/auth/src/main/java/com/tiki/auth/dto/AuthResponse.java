package com.tiki.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String role;
        private String sellerApplicationStatus; // PENDING, APPROVED, REJECTED, or null
        
        private String fullName;
        private Integer age;
        private String phoneNumber;
        private String address;
        private String gender;
        private String workplace;
        private Integer loyaltyPoints;
        private String loyaltyTier;
        
        public UserInfo(Long id, String username, String email, String role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }
}


