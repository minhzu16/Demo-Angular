package com.tiki.live.service;

import com.tiki.live.dto.LiveChatMessageDto;
import com.tiki.live.entity.LiveChatMessageEntity;
import com.tiki.live.repository.LiveChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiveChatService {

    private final LiveChatMessageRepository liveChatMessageRepository;

    @Transactional
    public LiveChatMessageDto postMessage(
            Long sessionId,
            Long userId,
            String userName,
            String message,
            LiveChatMessageEntity.MessageType type) {

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        if (message.length() > 500) {
            throw new IllegalArgumentException("Tin nhắn không được vượt quá 500 ký tự");
        }

        LiveChatMessageEntity entity = LiveChatMessageEntity.builder()
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName != null ? userName : "User#" + userId)
                .message(message.trim())
                .type(type != null ? type : LiveChatMessageEntity.MessageType.CHAT)
                .build();

        LiveChatMessageEntity saved = liveChatMessageRepository.save(entity);
        log.info("Chat in session {}: user={}, msg='{}'", sessionId, userName, message);
        return toDto(saved);
    }

    public Page<LiveChatMessageDto> getMessages(Long sessionId, Pageable pageable) {
        return liveChatMessageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable)
                .map(this::toDto);
    }

    private LiveChatMessageDto toDto(LiveChatMessageEntity e) {
        return LiveChatMessageDto.builder()
                .id(e.getId())
                .sessionId(e.getSessionId())
                .userId(e.getUserId())
                .userName(e.getUserName())
                .message(e.getMessage())
                .type(e.getType())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
