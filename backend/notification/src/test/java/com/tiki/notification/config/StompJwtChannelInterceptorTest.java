package com.tiki.notification.config;

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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StompJwtChannelInterceptorTest {

    private static final String SECRET = "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong12345678";
    private final StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(SECRET);

    private String token(long userId, String username, String role, long ttlMs) {
        return Jwts.builder()
                .setClaims(Map.of("userId", userId, "role", role))
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
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

    private static StompJwtChannelInterceptor.StompUser user(String id) {
        return new StompJwtChannelInterceptor.StompUser(id, "u" + id, "BUYER");
    }

    @Test
    @DisplayName("CONNECT không token / token sai / token hết hạn -> bị từ chối")
    void connect_withoutValidToken_isRejected() {
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), null));
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer garbage", null), null));
        String expired = token(7, "an", "BUYER", -60_000);
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer " + expired, null), null));
    }

    @Test
    @DisplayName("CONNECT với JWT hợp lệ -> principal là userId")
    void connect_withValidToken_setsPrincipal() {
        Message<byte[]> msg = frame(StompCommand.CONNECT, null, "Bearer " + token(7, "an", "BUYER", 60_000), null);
        interceptor.preSend(msg, null);

        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(msg, StompHeaderAccessor.class);
        StompJwtChannelInterceptor.StompUser u = assertInstanceOf(StompJwtChannelInterceptor.StompUser.class, acc.getUser());
        assertEquals("7", u.getName());
        assertEquals("an", u.username());
    }

    @Test
    @DisplayName("SUBSCRIBE: chỉ topic của chính mình; không thể nghe lén user khác hay topic lạ")
    void subscribe_isRestrictedToOwnTopics() {
        var me = user("7");
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/notifications/7", null, me), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/7", null, me), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/flash-sale", null, me), null);

        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/notifications/8", null, me), null));
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/8", null, me), null));
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/notifications/70", null, me), null));
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/secret", null, me), null));
    }

    @Test
    @DisplayName("SUBSCRIBE khi chưa xác thực -> bị từ chối")
    void subscribe_withoutAuthentication_isRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/flash-sale", null, null), null));
    }

    @Test
    @DisplayName("SEND: senderId/senderName bị ghi đè bằng danh tính đã xác thực, bỏ qua giá trị client giả mạo")
    void send_overwritesSpoofedSender() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.SEND);
        acc.setDestination("/app/chat/1");
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
