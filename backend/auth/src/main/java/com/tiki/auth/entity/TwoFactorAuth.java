package com.tiki.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "two_factor_auth")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorAuth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "secret_key", nullable = false)
    private String secretKey;
    
    @Column(nullable = false)
    private Boolean enabled = false;
    
    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
