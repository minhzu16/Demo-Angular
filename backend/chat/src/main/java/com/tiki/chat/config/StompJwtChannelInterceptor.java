package com.tiki.chat.config;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates and authorizes STOMP traffic for the chat service.
 *
 * Authentication: the same HS256 JWT the gateway validates, carried in the STOMP CONNECT frame
 * (browsers cannot set headers on a WebSocket handshake).
 *
 * Authorization — one private channel per CONVERSATION = (shop, buyer):
 *   /topic/conv/{shopId}/{buyerId}   the buyer themself, or the owner of that shop
 *   /topic/inbox/{shopId}            the owner of that shop only (copy of every conversation, for the seller UI)
 *   /topic/chat/{userId}             that user only
 *   /app/chat/{shopId}/{buyerId}[/join|/leave]   same rule as the conversation channel
 * The former shared /topic/shop/{shopId} channel delivered every buyer's messages to every subscriber and is gone.
 *
 * SEND frames get "senderId" / "senderName" overwritten with the authenticated identity.
 */
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    /** Authenticated STOMP user: name is the numeric user id as a string. */
    public record StompUser(String name, String username, String role) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }

    /** Resolves "does this user own this shop?" (fail closed on any error). */
    public interface ShopOwnership {
        boolean owns(StompUser user, Long shopId);
    }

    private static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/conv/(\\d+)/(\\d+)$");
    private static final Pattern INBOX_TOPIC = Pattern.compile("^/topic/inbox/(\\d+)$");
    private static final Pattern USER_CHAT_TOPIC = Pattern.compile("^/topic/chat/(\\d+)$");
    private static final Pattern CHAT_SEND = Pattern.compile("^/app/chat/(\\d+)/(\\d+)(/join|/leave)?$");

    private final Key key;
    private final ShopOwnership shopOwnership;

    public StompJwtChannelInterceptor(String jwtSecret, ShopOwnership shopOwnership) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.shopOwnership = shopOwnership;
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
            authorizeSend(user, accessor.getDestination());
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

    void authorizeSubscription(StompUser user, String destination) {
        if (destination == null) {
            throw new MessagingException("Forbidden: missing destination");
        }
        Matcher conv = CONVERSATION_TOPIC.matcher(destination);
        Matcher inbox = INBOX_TOPIC.matcher(destination);
        Matcher own = USER_CHAT_TOPIC.matcher(destination);
        if (conv.matches()) {
            requireConversationAccess(user, Long.parseLong(conv.group(1)), conv.group(2));
        } else if (inbox.matches()) {
            requireShopOwner(user, Long.parseLong(inbox.group(1)));
        } else if (own.matches()) {
            if (!user.name().equals(own.group(1))) {
                throw new MessagingException("Forbidden: this topic belongs to another user");
            }
        } else if (!destination.equals("/topic/flash-sale")) {
            throw new MessagingException("Forbidden: cannot subscribe to " + destination);
        }
    }

    void authorizeSend(StompUser user, String destination) {
        Matcher m = destination == null ? null : CHAT_SEND.matcher(destination);
        if (m == null || !m.matches()) {
            throw new MessagingException("Forbidden: cannot send to " + destination);
        }
        requireConversationAccess(user, Long.parseLong(m.group(1)), m.group(2));
    }

    /** The buyer of the conversation, or the owner of the shop it belongs to. */
    private void requireConversationAccess(StompUser user, Long shopId, String buyerId) {
        if (user.name().equals(buyerId)) {
            return;
        }
        requireShopOwner(user, shopId);
    }

    private void requireShopOwner(StompUser user, Long shopId) {
        if (!shopOwnership.owns(user, shopId)) {
            throw new MessagingException("Forbidden: you do not own shop " + shopId);
        }
    }
}
