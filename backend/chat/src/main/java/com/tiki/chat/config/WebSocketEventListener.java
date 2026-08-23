package com.tiki.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ BUG 25 FIX: Detect WebSocket disconnections (tab close, network drop, etc.)
 * and broadcast a LEAVE message to the chat channel so other participants
 * see the user as "offline" immediately instead of waiting for a heartbeat timeout.
 */
@Component
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // Track sessionId -> {userId, shopId, senderName} for disconnect notification
    private final Map<String, Map<String, Object>> sessionRegistry = new ConcurrentHashMap<>();

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Called by Spring whenever a STOMP session is established.
     * Register user session metadata for later disconnect handling.
     */
    public void registerSession(String sessionId, Long userId, Long shopId, String senderName) {
        sessionRegistry.put(sessionId, Map.of(
            "userId", userId,
            "shopId", shopId,
            "senderName", senderName
        ));
        log.debug("WS session registered: sessionId={}, userId={}, shopId={}", sessionId, userId, shopId);
    }

    /**
     * ✅ BUG 25 FIX: Fires when the browser closes the tab, drops the network,
     * or calls disconnect() explicitly. Broadcasts a system LEAVE message to the
     * shop channel so the UI can update presence indicators immediately.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Map<String, Object> sessionData = sessionRegistry.remove(sessionId);
        if (sessionData == null) {
            // Session was not tracked (e.g., anonymous ping) — nothing to broadcast
            return;
        }

        Long shopId = (Long) sessionData.get("shopId");
        String senderName = (String) sessionData.get("senderName");

        log.info("WS disconnect: sessionId={}, user={} left shop={} chat", sessionId, senderName, shopId);

        Map<String, Object> leaveMsg = Map.of(
            "type", "LEAVE",
            "sender", senderName != null ? senderName : "User",
            "content", (senderName != null ? senderName : "User") + " đã rời khỏi cuộc trò chuyện",
            "shopId", shopId != null ? shopId : 0
        );

        if (shopId != null) {
            messagingTemplate.convertAndSend("/topic/shop/" + shopId, leaveMsg);
        }
    }
}
