package com.tiki.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId; // The owner/recipient of the notification
    
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String type; // ORDER_STATUS, PROMOTION, SYSTEM, MESSAGE
    
    private boolean isRead;
    
    private String targetUrl; // URL to redirect when clicked
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
