package com.tiki.notification.controller;

import com.tiki.notification.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }
        
        log.info("Received chat message from {} to {}: {}", 
                 chatMessage.getSenderId(), 
                 chatMessage.getReceiverId(), 
                 chatMessage.getContent());
                 
        // Route the message to the specific receiver's topic
        String destination = "/topic/chat/" + chatMessage.getReceiverId();
        messagingTemplate.convertAndSend(destination, chatMessage);
        
        // Also send it back to the sender's topic so they can see their own message if they have multiple devices open
        String senderDestination = "/topic/chat/" + chatMessage.getSenderId();
        messagingTemplate.convertAndSend(senderDestination, chatMessage);
    }
}
