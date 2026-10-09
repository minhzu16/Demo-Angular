package com.tiki.chat.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StompJwtChannelInterceptorTest {

    private static final String SECRET = "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong12345678";
    /** Seller user 50 owns shop 5; nobody else owns anything. */
    private final StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(SECRET,
            (user, shopId) -> "50".equals(user.name()) && Long.valueOf(5L).equals(shopId));

    private static StompJwtChannelInterceptor.StompUser user(String id) {
        return new StompJwtChannelInterceptor.StompUser(id, "u" + id, "BUYER");
    }

    private Message<byte[]> frame(StompCommand command, String destination, String authorization,
                                  StompJwtChannelInterceptor.StompUser user) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(command);
        if (destination != null) acc.setDestination(destination);
        if (authorization != null) acc.setNativeHeader("Authorization", authorization);
        if (user != null) acc.setUser(user);
        acc.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
    }

    private void subscribe(StompJwtChannelInterceptor.StompUser u, String destination) {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, null, u), null);
    }

    private void send(StompJwtChannelInterceptor.StompUser u, String destination) {
        interceptor.preSend(frame(StompCommand.SEND, destination, null, u), null);
    }

    @Test
    @DisplayName("CONNECT cần JWT hợp lệ")
    void connect_requiresValidToken() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), null));
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer nope", null), null));

        String token = Jwts.builder().setClaims(Map.of("userId", 7L, "role", "BUYER")).setSubject("an")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
        Message<byte[]> ok = frame(StompCommand.CONNECT, null, "Bearer " + token, null);
        interceptor.preSend(ok, null);
        assertEquals("7", MessageHeaderAccessor.getAccessor(ok, StompHeaderAccessor.class).getUser().getName());
    }

    @Test
    @DisplayName("Kênh hội thoại: chỉ người mua của hội thoại đó hoặc chủ shop")
    void conversationChannel_isPrivate() {
        subscribe(user("7"), "/topic/conv/5/7");   // the buyer
        subscribe(user("50"), "/topic/conv/5/7");  // the shop owner
        subscribe(user("50"), "/topic/conv/5/8");  // owner sees every conversation of their shop

        assertThrows(MessagingException.class, () -> subscribe(user("8"), "/topic/conv/5/7"));   // another buyer
        assertThrows(MessagingException.class, () -> subscribe(user("50"), "/topic/conv/6/7"));  // owner of a different shop
        assertThrows(MessagingException.class, () -> subscribe(user("7"), "/topic/conv/6/8"));
    }

    @Test
    @DisplayName("Inbox của shop chỉ dành cho chủ shop; kênh chung cũ /topic/shop/** bị loại bỏ")
    void inbox_isOwnerOnly_andSharedTopicIsGone() {
        subscribe(user("50"), "/topic/inbox/5");
        assertThrows(MessagingException.class, () -> subscribe(user("7"), "/topic/inbox/5"));
        assertThrows(MessagingException.class, () -> subscribe(user("50"), "/topic/inbox/6"));
        assertThrows(MessagingException.class, () -> subscribe(user("7"), "/topic/shop/5"));
        assertThrows(MessagingException.class, () -> subscribe(user("50"), "/topic/shop/5"));
    }

    @Test
    @DisplayName("Chỉ gửi vào hội thoại của mình; chat người mua không thể chen vào hội thoại khác")
    void send_isRestrictedToOwnConversations() {
        send(user("7"), "/app/chat/5/7");
        send(user("7"), "/app/chat/5/7/join");
        send(user("50"), "/app/chat/5/7");   // seller replying

        assertThrows(MessagingException.class, () -> send(user("8"), "/app/chat/5/7"));
        assertThrows(MessagingException.class, () -> send(user("7"), "/app/chat/5"));          // old shared destination
        assertThrows(MessagingException.class, () -> send(user("7"), "/app/other"));
        assertThrows(MessagingException.class, () -> send(null, "/app/chat/5/7"));
    }

    @Test
    @DisplayName("SEND: senderId/senderName bị ghi đè bằng danh tính từ token")
    void send_stampsVerifiedSender() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.SEND);
        acc.setDestination("/app/chat/5/7");
        acc.setUser(user("7"));
        acc.setNativeHeader("senderId", "999");
        acc.setNativeHeader("senderName", "admin");
        acc.setLeaveMutable(true);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        interceptor.preSend(msg, null);

        StompHeaderAccessor out = MessageHeaderAccessor.getAccessor(msg, StompHeaderAccessor.class);
        assertEquals("7", out.getFirstNativeHeader("senderId"));
        assertEquals("u7", out.getFirstNativeHeader("senderName"));
    }
}
