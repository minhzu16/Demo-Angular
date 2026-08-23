package com.tiki.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket STOMP message DTO for real-time chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

    private MessageType type;

    private String content;

    private String sender;

    /**
     * NOTE: senderId is SET BY SERVER (ChatController) from validated STOMP header.
     * Client-supplied senderId in the body is intentionally overwritten to prevent spoofing.
     */
    private Long senderId;

    private Long shopId;
    private String shopName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
