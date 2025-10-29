package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {
    
    public enum EventType {
        SUSPICIOUS_LOGIN,
        PASSWORD_CHANGED,
        TWO_FA_ENABLED,
        TWO_FA_DISABLED,
        EMAIL_VERIFIED,
        OAUTH_LINKED,
        SESSION_REVOKED,
        FAILED_LOGIN_ATTEMPT,
        ACCOUNT_LOCKED
    }
    
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(columnDefinition = "JSON")
    private String metadata;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
