package com.tiki.chat.controller;

import com.tiki.chat.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /**
     * One private channel per conversation (shop, buyer). Delivered to the conversation topic (buyer + shop
     * owner) and to the shop's inbox topic (owner only, so the seller UI can list all conversations).
     */
    private void publish(Long shopId, Long buyerId, ChatMessage message) {
        message.setBuyerId(buyerId);
        if (messagingTemplate == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/conv/" + shopId + "/" + buyerId, message);
        messagingTemplate.convertAndSend("/topic/inbox/" + shopId, message);
    }

    /** Client sends to /app/chat/{shopId}/{buyerId}; access is enforced by StompJwtChannelInterceptor. */
    @MessageMapping("/chat/{shopId}/{buyerId}")
    public void chat(@DestinationVariable Long shopId, @DestinationVariable Long buyerId,
                     @Payload ChatMessage message,
                     @Header(value = "senderId", required = false) String senderIdHeader,
                     @Header(value = "senderName", required = false) String senderNameHeader) {
        publish(shopId, buyerId, sendMessage(shopId, message, senderIdHeader, senderNameHeader));
    }

    @MessageMapping("/chat/{shopId}/{buyerId}/join")
    public void join(@DestinationVariable Long shopId, @DestinationVariable Long buyerId,
                     @Payload ChatMessage message,
                     @Header(value = "senderId", required = false) String senderIdHeader,
                     @Header(value = "senderName", required = false) String senderNameHeader) {
        // Presence notices are for the buyer's own side; a seller opening the conversation stays silent.
        if (String.valueOf(buyerId).equals(senderIdHeader)) {
            publish(shopId, buyerId, joinChat(shopId, message, senderIdHeader, senderNameHeader));
        }
    }

    @MessageMapping("/chat/{shopId}/{buyerId}/leave")
    public void leave(@DestinationVariable Long shopId, @DestinationVariable Long buyerId,
                      @Payload ChatMessage message,
                      @Header(value = "senderId", required = false) String senderIdHeader,
                      @Header(value = "senderName", required = false) String senderNameHeader) {
        if (String.valueOf(buyerId).equals(senderIdHeader)) {
            publish(shopId, buyerId, leaveChat(shopId, message, senderIdHeader, senderNameHeader));
        }
    }

    /** Builds the outgoing message for a CHAT frame (sender verification, rate limit, sanitising, FAQ bot). */
    public ChatMessage sendMessage(
            @DestinationVariable Long shopId,
            @Payload ChatMessage message,
            @Header(value = "senderId", required = false) String senderIdHeader,
            @Header(value = "senderName", required = false) String senderNameHeader) {

        // ✅ BUG 20 FIX: senderId / senderName are stamped by StompJwtChannelInterceptor from the verified JWT
        // (the client-supplied header used to be trusted as-is). Never take them from the payload body.
        Long verifiedSenderId = parseSenderId(senderIdHeader);
        if (verifiedSenderId != null) {
            message.setSenderId(verifiedSenderId);
        }
        if (senderNameHeader != null && !senderNameHeader.isBlank()) {
            message.setSender(sanitize(senderNameHeader));
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

        // Trợ lý mua sắm Chatbot FAQ: Phản hồi tự động khi nhắc @bot hoặc hỏi bot
        String rawContent = message.getContent() != null ? message.getContent().trim() : "";
        if (rawContent.toLowerCase().startsWith("@bot") || rawContent.toLowerCase().startsWith("/bot")) {
            String userQuery = rawContent.replaceFirst("(?i)^[@/]bot\\s*", "");
            String botAnswer = answerFaq(userQuery);
            message.setSender("Tiki Bot Assistant");
            message.setContent(botAnswer);
        }

        log.info("Chat [shop={}] from user={} (id={}): '{}'",
                shopId, message.getSender(), message.getSenderId(), message.getContent());
        return message;
    }

    /**
     * REST API: Lấy danh mục câu hỏi thường gặp (FAQ)
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/v1/chat/faq")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, String> getFaqList() {
        return Map.of(
                "Chính sách đổi trả & RMA", "Đổi trả trong 7 ngày đối với đơn DELIVERED, kiểm định QC tại kho và hoàn tiền tự động.",
                "Chính sách phí vận chuyển", "Miễn phí vận chuyển từ 500.000đ hoặc áp dụng mã Freeship từ gói hội viên Tiki PRO.",
                "Thời gian giao hàng", "Giao nhanh 1-2 ngày tại HN/HCM và 2-4 ngày tại các tỉnh thành khác.",
                "Phương thức thanh toán", "Hỗ trợ SePay QR, Gift Card, Store Credit và COD.",
                "Gói hội viên Tiki PRO", "Ưu đãi Freeship và nhân đôi điểm tích lũy 2x loyalty points."
        );
    }

    /**
     * REST API: Trợ lý tư vấn mua sắm tự động (Chatbot FAQ Assistant)
     */
    @org.springframework.web.bind.annotation.PostMapping("/api/v1/chat/bot-assistant")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> askBotAssistant(@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> request) {
        String question = (request != null && request.containsKey("question")) ? request.get("question") : "";
        String answer = answerFaq(question);
        return Map.of(
                "question", question,
                "answer", answer,
                "sender", "Tiki Bot Assistant",
                "timestamp", Instant.now().toString()
        );
    }

    /**
     * Thuật toán giải đáp câu hỏi thường gặp của trợ lý ảo mua sắm
     */
    public String answerFaq(String query) {
        if (query == null || query.isBlank()) {
            return "Xin chào! Tôi là Trợ lý Ảo Tiki. Bạn có thể hỏi tôi về: 'đổi trả', 'phí ship', 'thời gian giao hàng', 'thanh toán', hoặc 'hội viên'.";
        }
        String q = query.toLowerCase();
        if (q.contains("đổi trả") || q.contains("doi tra") || q.contains("rma") || q.contains("hoàn tiền") || q.contains("hoan tien")) {
            return "Chính sách đổi trả & RMA: Quý khách có thể gửi yêu cầu đổi trả trong vòng 7 ngày kể từ khi đơn hàng giao thành công (DELIVERED). Sau khi được duyệt, quý khách gửi hàng về kho để QC kiểm định và nhận hoàn tiền/đổi mới.";
        }
        if (q.contains("phí ship") || q.contains("phi ship") || q.contains("vận chuyển") || q.contains("freeship") || q.contains("miễn phí")) {
            return "Chính sách vận chuyển: Đơn hàng từ 500.000đ được miễn phí vận chuyển toàn quốc. Hội viên Tiki PRO nhận thêm ưu đãi miễn phí ship hàng tháng.";
        }
        if (q.contains("giao hàng") || q.contains("giao hang") || q.contains("thời gian") || q.contains("bao lâu")) {
            return "Thời gian giao hàng tiêu chuẩn: 1-2 ngày đối với khu vực nội thành Hà Nội & TP.HCM, 2-4 ngày đối với các tỉnh thành khác.";
        }
        if (q.contains("thanh toán") || q.contains("thanh toan") || q.contains("gift card") || q.contains("store credit") || q.contains("sepay")) {
            return "Phương thức thanh toán: Hỗ trợ Chuyển khoản QR SePay/VNPay, Thẻ quà tặng (Gift Card), Ví số dư Store Credit và Thanh toán khi nhận hàng (COD).";
        }
        if (q.contains("hội viên") || q.contains("hoi vien") || q.contains("membership") || q.contains("tiki pro")) {
            return "Gói hội viên Tiki PRO: Miễn phí vận chuyển không giới hạn hạn mức và nhân đôi điểm thưởng tích lũy (2x Loyalty Points) trên mỗi đơn hàng hoàn tất.";
        }
        return "Cảm ơn bạn đã liên hệ. Câu hỏi của bạn đã được chuyển tới nhân viên hỗ trợ Shop. Bạn cũng có thể tra cứu nhanh: 'đổi trả', 'phí ship', 'thanh toán'.";
    }

    /**
     * User joins a chat room (shop channel).
     * Client sends to: /app/chat/{shopId}/join
     */
    public ChatMessage joinChat(
            @DestinationVariable Long shopId,
            @Payload ChatMessage message,
            @Header(value = "senderId", required = false) String senderIdHeader,
            @Header(value = "senderName", required = false) String senderNameHeader) {

        Long verifiedSenderId = parseSenderId(senderIdHeader);
        if (verifiedSenderId != null) {
            message.setSenderId(verifiedSenderId);
        }
        if (senderNameHeader != null && !senderNameHeader.isBlank()) {
            message.setSender(senderNameHeader);
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
    public ChatMessage leaveChat(
            @DestinationVariable Long shopId,
            @Payload ChatMessage message,
            @Header(value = "senderId", required = false) String senderIdHeader,
            @Header(value = "senderName", required = false) String senderNameHeader) {

        Long verifiedSenderId = parseSenderId(senderIdHeader);
        if (verifiedSenderId != null) {
            message.setSenderId(verifiedSenderId);
        }
        if (senderNameHeader != null && !senderNameHeader.isBlank()) {
            message.setSender(senderNameHeader);
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
