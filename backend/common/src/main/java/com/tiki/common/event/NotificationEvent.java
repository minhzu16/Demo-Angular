package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification Event (Email/SMS)
 * Sprint 9 - Day 7-9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String type; // EMAIL, SMS
    private String recipient; // email or phone
    private String subject;
    private String template;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}
