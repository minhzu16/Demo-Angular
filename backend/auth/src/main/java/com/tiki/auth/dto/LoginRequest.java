package com.tiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    private String password;

    /** TOTP or backup code; required only for accounts with 2FA enabled. */
    private String twoFactorCode;
}


