package com.tiki.notification.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.Principal;

/**
 * Authenticates STOMP sessions with the same HS256 JWT the gateway validates for REST.
 *
 * Browsers cannot set an Authorization header on a WebSocket handshake, and the gateway therefore cannot
 * inject identity for /ws routes — so the token travels in the STOMP CONNECT frame instead. Without this, anyone
 * who knew a user id could SUBSCRIBE to /topic/notifications/{id} (or /topic/chat/{id}) and read that user's
 * messages, and the "senderId" header on SEND frames was whatever the client wrote.
 *
 *  - CONNECT   : requires a valid "Authorization: Bearer ..." native header; the user becomes the session principal.
 *  - SUBSCRIBE : per-user topics only for their owner; flash-sale is public to signed-in users; unknown topics denied.
 *  - SEND      : "senderId" is overwritten with the authenticated user id.
 *
 * (A copy lives in the chat service, which does not depend on the common module.)
 */
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    /** Authenticated STOMP user: name is the numeric user id as a string. */
    public record StompUser(String name, String username, String role) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }

    private static final String USER_TOPIC_PREFIX_NOTIFICATIONS = "/topic/notifications/";
    private static final String USER_TOPIC_PREFIX_CHAT = "/topic/chat/";

    private final Key key;

    public StompJwtChannelInterceptor(String jwtSecret) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(requireUser(accessor), accessor.getDestination());
        } else if (StompCommand.SEND.equals(command)) {
            StompUser user = requireUser(accessor);
            accessor.setNativeHeader("senderId", user.name());
            if (user.username() != null) {
                accessor.setNativeHeader("senderName", user.username());
            }
        }
        return message;
    }

    private StompUser authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new MessagingException("Unauthorized: missing bearer token");
        }
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(header.substring(7).trim()).getBody();
            Object userId = claims.get("userId");
            if (userId == null) {
                throw new MessagingException("Unauthorized: token has no userId");
            }
            Object role = claims.get("role");
            return new StompUser(String.valueOf(userId), claims.getSubject(), role != null ? String.valueOf(role) : "BUYER");
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessagingException("Unauthorized: invalid or expired token");
        }
    }

    private StompUser requireUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompUser user) {
            return user;
        }
        throw new MessagingException("Unauthorized: not authenticated");
    }

    static void authorizeSubscription(StompUser user, String destination) {
        if (destination == null) {
            throw new MessagingException("Forbidden: missing destination");
        }
        if (destination.startsWith(USER_TOPIC_PREFIX_NOTIFICATIONS)) {
            requireOwner(user, destination.substring(USER_TOPIC_PREFIX_NOTIFICATIONS.length()));
        } else if (destination.startsWith(USER_TOPIC_PREFIX_CHAT)) {
            requireOwner(user, destination.substring(USER_TOPIC_PREFIX_CHAT.length()));
        } else if (!destination.equals("/topic/flash-sale")) {
            throw new MessagingException("Forbidden: cannot subscribe to " + destination);
        }
    }

    private static void requireOwner(StompUser user, String ownerId) {
        if (!user.name().equals(ownerId)) {
            throw new MessagingException("Forbidden: this topic belongs to another user");
        }
    }
}
