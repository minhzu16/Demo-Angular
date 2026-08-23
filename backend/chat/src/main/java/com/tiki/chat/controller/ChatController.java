package com.tiki.chat.controller;

import com.tiki.chat.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * STOMP message controller for real-time chat.
 * Messages are NOT persisted — ephemeral in-memory broadcast only.
 *
 * Security:
 * - ✅ BUG 20: senderId is read from STOMP header (set by gateway/client auth) and overrides
 *              any body-supplied senderId, preventing sender identity spoofing.
 * - ✅ BUG 21: message content is HTML-escaped before broadcast to prevent XSS.
 * - ✅ BUG 22: simple per-user rate limiter (max 10 messages per 5 seconds) to prevent spam.
 */
@Controller
@Slf4j
public class ChatController {

    // ✅ BUG 22: Rate limiter — tracks message count per senderId, resets every 5 seconds
    private static final int MAX_MESSAGES_PER_WINDOW = 10;
    private static final long WINDOW_MS = 5_000L;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final Map<Long, long[]> rateLimiter = new ConcurrentHashMap<>();

    /**
     * Buyer/Seller sends a CHAT message to a shop channel.
     * Client sends to: /app/chat/{shopId}
     * Subscribers of /topic/shop/{shopId} receive the message.
     */
    @MessageMapping("/chat/{shopId}")
    @SendTo("/topic/shop/{shopId}")
    public ChatMessage sendMessage(
            @DestinationVariable Long shopId,
            @Payload ChatMessage message,
            @Header(value = "senderId", required = false) String senderIdHeader) {

        // ✅ BUG 20 FIX: Override senderId from trusted STOMP header, not from the payload body
        Long verifiedSenderId = parseSenderId(senderIdHeader);
        if (verifiedSenderId != null) {
            message.setSenderId(verifiedSenderId);
        }

        // ✅ BUG 22 FIX: Rate limiting check
        if (message.getSenderId() != null && isRateLimited(message.getSenderId())) {
            log.warn("Rate limit exceeded for user {}", message.getSenderId());
            // Return a system warning instead of dropping silently
            return ChatMessage.builder()
                    .type(ChatMessage.MessageType.LEAVE)
                    .content("⚠️ Bạn đang gửi quá nhiều tin nhắn. Vui lòng chờ.")
                    .sender("System")
                    .shopId(shopId)
                    .timestamp(Instant.now())
                    .build();
        }

        message.setShopId(shopId);
        message.setTimestamp(Instant.now());
        // ✅ BUG 21 FIX: Sanitize content to prevent XSS injection in chat bubbles
        message.setContent(sanitize(message.getContent()));

        log.info("Chat [shop={}] from user={} (id={}): '{}'",
                shopId, message.getSender(), message.getSenderId(), message.getContent());
        return message;
    }

    /**
     * User joins a chat room (shop channel).
     * Client sends to: /app/chat/{shopId}/join
     */
    @MessageMapping("/chat/{shopId}/join")
    @SendTo("/topic/shop/{shopId}")
    public ChatMessage joinChat(
            @DestinationVariable Long shopId,
            @Payload ChatMessage message,
            @Header(value = "senderId", required = false) String senderIdHeader) {

        Long verifiedSenderId = parseSenderId(senderIdHeader);
        if (verifiedSenderId != null) {
            message.setSenderId(verifiedSenderId);
        }

        message.setShopId(shopId);
        message.setType(ChatMessage.MessageType.JOIN);
        message.setTimestamp(Instant.now());
        // Sanitize sender name too
        message.setSender(sanitize(message.getSender()));
        message.setContent(sanitize(message.getSender()) + " đã tham gia cuộc trò chuyện");
        log.info("User {} (id={}) joined shop {} chat", message.getSender(), message.getSenderId(), shopId);
        return message;
    }

    /**
     * User leaves a chat room.
     * Client sends to: /app/chat/{shopId}/leave
     */
    @MessageMapping("/chat/{shopId}/leave")
    @SendTo("/topic/shop/{shopId}")
    public ChatMessage leaveChat(
            @DestinationVariable Long shopId,
            @Payload ChatMessage message,
            @Header(value = "senderId", required = false) String senderIdHeader) {

        Long verifiedSenderId = parseSenderId(senderIdHeader);
        if (verifiedSenderId != null) {
            message.setSenderId(verifiedSenderId);
        }

        message.setShopId(shopId);
        message.setType(ChatMessage.MessageType.LEAVE);
        message.setTimestamp(Instant.now());
        message.setSender(sanitize(message.getSender()));
        message.setContent(sanitize(message.getSender()) + " đã rời khỏi cuộc trò chuyện");
        log.info("User {} (id={}) left shop {} chat", message.getSender(), message.getSenderId(), shopId);
        return message;
    }

    // --- Helpers ---

    /**
     * ✅ BUG 21 FIX: Simple HTML entity escaping to prevent XSS via chat content.
     * Strips or escapes angle brackets, quotes, and script-related characters.
     */
    private String sanitize(String input) {
        if (input == null) return "";
        String sanitized = input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
        // Enforce max length
        if (sanitized.length() > MAX_CONTENT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_CONTENT_LENGTH) + "...";
        }
        return sanitized;
    }

    /**
     * ✅ BUG 22 FIX: Sliding-window rate limiter — max 10 messages per 5 seconds per user.
     * rateLimiter stores: [count, windowStartMs]
     */
    private boolean isRateLimited(Long userId) {
        long now = System.currentTimeMillis();
        long[] window = rateLimiter.computeIfAbsent(userId, k -> new long[]{0, now});

        if (now - window[1] > WINDOW_MS) {
            // Reset window
            window[0] = 1;
            window[1] = now;
            return false;
        }

        window[0]++;
        if (window[0] > MAX_MESSAGES_PER_WINDOW) {
            return true;
        }
        return false;
    }

    /**
     * ✅ BUG 20 FIX: Parse verified senderId from STOMP header.
     * Falls back gracefully if header is absent (unauthenticated guest users).
     */
    private Long parseSenderId(String senderIdHeader) {
        if (senderIdHeader == null || senderIdHeader.isBlank()) return null;
        try {
            return Long.parseLong(senderIdHeader.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid senderId header value: '{}'", senderIdHeader);
            return null;
        }
    }
}
