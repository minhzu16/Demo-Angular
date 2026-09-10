package com.tiki.live.dto;

import com.tiki.live.entity.LiveChatMessageEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatMessageDto {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String userName;

    @NotBlank(message = "Tin nhắn không được để trống")
    private String message;

    private LiveChatMessageEntity.MessageType type;
    private LocalDateTime createdAt;
}
