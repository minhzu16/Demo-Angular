package com.tiki.chat.controller;

import com.tiki.chat.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatControllerTest {

    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatController = new ChatController();
    }

    @Test
    void testSendMessage_NormalChat() {
        ChatMessage msg = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHAT)
                .content("Shop ơi sản phẩm này còn màu đen không?")
                .sender("User1")
                .senderId(99L)
                .build();

        ChatMessage result = chatController.sendMessage(1L, msg, "123");

        assertNotNull(result);
        assertEquals(1L, result.getShopId());
        assertEquals(123L, result.getSenderId(), "SenderId should be overridden by trusted header");
        assertEquals("Shop ơi sản phẩm này còn màu đen không?", result.getContent());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSendMessage_SanitizesXss() {
        ChatMessage msg = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHAT)
                .content("<script>alert('hack')</script>")
                .sender("Attacker")
                .build();

        ChatMessage result = chatController.sendMessage(1L, msg, "100");

        assertFalse(result.getContent().contains("<script>"));
        assertTrue(result.getContent().contains("&lt;script&gt;"));
    }

    @Test
    void testSendMessage_RateLimiter() {
        // Send 10 messages within window
        for (int i = 0; i < 10; i++) {
            ChatMessage msg = ChatMessage.builder()
                    .type(ChatMessage.MessageType.CHAT)
                    .content("Spam message " + i)
                    .sender("Spammer")
                    .build();
            chatController.sendMessage(1L, msg, "555");
        }

        // 11th message should be rate limited
        ChatMessage spamMsg = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHAT)
                .content("Spam message 11")
                .sender("Spammer")
                .build();
        ChatMessage blocked = chatController.sendMessage(1L, spamMsg, "555");

        assertEquals("System", blocked.getSender());
        assertTrue(blocked.getContent().contains("quá nhiều tin nhắn"));
    }

    @Test
    void testSendMessage_BotAssistant_ReturnsFaqAnswer() {
        ChatMessage msg = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHAT)
                .content("@bot chính sách đổi trả hàng như thế nào?")
                .sender("Buyer1")
                .build();

        ChatMessage result = chatController.sendMessage(1L, msg, "200");

        assertEquals("Tiki Bot Assistant", result.getSender());
        assertTrue(result.getContent().contains("Chính sách đổi trả & RMA"));
    }

    @Test
    void testAskBotAssistant_RestEndpoint() {
        Map<String, Object> res = chatController.askBotAssistant(Map.of("question", "phí ship bao nhiêu"));

        assertEquals("Tiki Bot Assistant", res.get("sender"));
        assertTrue(res.get("answer").toString().contains("Chính sách vận chuyển"));
    }

    @Test
    void testGetFaqList_RestEndpoint() {
        Map<String, String> faqs = chatController.getFaqList();

        assertNotNull(faqs);
        assertTrue(faqs.containsKey("Chính sách đổi trả & RMA"));
        assertTrue(faqs.containsKey("Gói hội viên Tiki PRO"));
    }
}
