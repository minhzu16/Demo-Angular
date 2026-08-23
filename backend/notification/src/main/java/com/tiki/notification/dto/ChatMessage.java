package com.tiki.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long senderId;
    private Long receiverId;
    private String content;
    private String senderName; // To display on UI
    private LocalDateTime timestamp;
}
