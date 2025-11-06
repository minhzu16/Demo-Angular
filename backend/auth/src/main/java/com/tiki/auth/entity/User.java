package com.tiki.auth.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    
    public enum Role { 
        BUYER,
        SELLER,
        ADMIN;
        
        @JsonCreator
        public static Role fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return BUYER;
            }
            
            String upperValue = value.trim().toUpperCase();
            
            // Map legacy values
            if ("USER".equals(upperValue) || "CUSTOMER".equals(upperValue)) {
                return BUYER;
            }
            
            try {
                return Role.valueOf(upperValue);
            } catch (IllegalArgumentException e) {
                return BUYER;
            }
        }
        
        @JsonValue
        public String toValue() {
            return this.name();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.BUYER; // Legacy field, kept for backward compatibility
    
    @Transient
    private Set<Role> roles; // Multi-role support

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    // Multi-role helper methods
    public Set<Role> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
            roles.add(role); // Fallback to legacy role
        }
        return roles;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
        // Update legacy role field to primary role
        if (roles != null && !roles.isEmpty()) {
            if (roles.contains(Role.ADMIN)) {
                this.role = Role.ADMIN;
            } else if (roles.contains(Role.SELLER)) {
                this.role = Role.SELLER;
            } else {
                this.role = Role.BUYER;
            }
        }
    }
    
    public boolean hasRole(Role role) {
        return getRoles().contains(role);
    }
    
    public void addRole(Role role) {
        getRoles().add(role);
        setRoles(roles); // Update legacy field
    }
    
    public void removeRole(Role role) {
        getRoles().remove(role);
        setRoles(roles); // Update legacy field
    }
    
    public String getRolesAsString() {
        return getRoles().stream()
                .map(Role::name)
                .sorted()
                .collect(Collectors.joining(","));
    }
    
    // Helper methods for password (alias for passwordHash)
    public String getPassword() { return passwordHash; }
    public void setPassword(String password) { this.passwordHash = password; }
}


