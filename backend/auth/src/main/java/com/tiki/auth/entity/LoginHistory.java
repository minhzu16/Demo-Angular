package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {
    
    public enum Status {
        SUCCESS,
        FAILED,
        BLOCKED
    }
    
    public enum LoginMethod {
        PASSWORD,
        OAUTH_GOOGLE,
        OAUTH_FACEBOOK,
        OTP
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(length = 255)
    private String location;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "login_method", length = 50)
    private LoginMethod loginMethod;
    
    @Column(name = "logged_in_at")
    private LocalDateTime loggedInAt = LocalDateTime.now();
}
