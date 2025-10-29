package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "active_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;
    
    @Column(name = "refresh_token")
    private String refreshToken;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt = LocalDateTime.now();
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    public boolean isActive() {
        return revokedAt == null && LocalDateTime.now().isBefore(expiresAt);
    }
}
