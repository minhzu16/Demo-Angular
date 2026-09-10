package com.tiki.live.service;

import com.tiki.live.dto.LiveChatMessageDto;
import com.tiki.live.entity.LiveChatMessageEntity;
import com.tiki.live.repository.LiveChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveChatServiceTest {

    @Mock
    private LiveChatMessageRepository liveChatMessageRepository;

    @InjectMocks
    private LiveChatService liveChatService;

    @Test
    void testPostMessage_Success() {
        when(liveChatMessageRepository.save(any(LiveChatMessageEntity.class))).thenAnswer(inv -> {
            LiveChatMessageEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        LiveChatMessageDto res = liveChatService.postMessage(
                1L, 10L, "ViewerA", "Sản phẩm này còn màu đen không shop?",
                LiveChatMessageEntity.MessageType.CHAT
        );

        assertNotNull(res);
        assertEquals(10L, res.getId());
        assertEquals("ViewerA", res.getUserName());
        assertEquals("Sản phẩm này còn màu đen không shop?", res.getMessage());
    }

    @Test
    void testPostMessage_Empty_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                liveChatService.postMessage(1L, 10L, "User", "   ", LiveChatMessageEntity.MessageType.CHAT));
    }

    @Test
    void testPostMessage_TooLong_Throws() {
        String longMsg = "A".repeat(501);
        assertThrows(IllegalArgumentException.class, () ->
                liveChatService.postMessage(1L, 10L, "User", longMsg, LiveChatMessageEntity.MessageType.CHAT));
    }
}
