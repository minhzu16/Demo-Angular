package com.tiki.notification.service;

import com.tiki.common.event.NotificationEvent;
import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.common.event.FlashSaleStockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {
    private final NotificationService notificationService;
    private final com.tiki.notification.repository.NotificationRepository notificationRepository; // For direct DB link if needed

    @RabbitListener(queues = "notification.queue")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        if (event.getUserId() != null) {
            notificationService.sendNotification(
                    event.getUserId().longValue(),
                    "Order Created",
                    "Your order with ID " + event.getOrderId() + " has been placed successfully. Thank you!",
                    "ORDER_STATUS",
                    "/orders/" + event.getOrderId()
            );
        }
    }

    @RabbitListener(queues = "notification.queue")
    public void consumeGenericNotification(Map<String, Object> message) {
        log.info("Received generic notification: {}", message);
        
        Object userIdObj = message.get("userId");
        String title = (String) message.get("title");
        String msg = (String) message.get("message");
        String type = (String) message.get("type");
        String link = (String) message.get("link");

        if (userIdObj != null && title != null && msg != null) {
            Long userId = Long.valueOf(userIdObj.toString());
            notificationService.sendNotification(userId, title, msg, 
                type != null ? type : "SYSTEM", 
                link != null ? link : "/");
        }
    }

    @RabbitListener(queues = "notification.queue")
    public void consumeStockEvent(Map<String, Object> message) {
        // Filter for stock events
        if (message.containsKey("status") && message.containsKey("orderId")) {
            Integer orderId = (Integer) message.get("orderId");
            String status = (String) message.get("status");
            
            log.info("Notification received stock update for order {}: status {}", orderId, status);
            
            // In a production app, we would fetch the userId from the order-service here.
            // For this modernized demo, we'll assume the message could include userId or we mock it.
            // We'll skip notification if userId is missing to avoid errors, or use a hardcoded demo userId (e.g. 1) if testing.
            
            Object userIdObj = message.get("userId");
            if (userIdObj != null) {
                Long userId = Long.valueOf(userIdObj.toString());
                String title = "RESERVED".equals(status) ? "Order Confirmed" : "Order Cancelled";
                String msg = "RESERVED".equals(status) ? 
                    "Great news! Order #" + orderId + " has been confirmed and is being prepared." :
                    "We're sorry, Order #" + orderId + " was cancelled due to stock unavailability.";
                
                notificationService.sendNotification(userId, title, msg, "ORDER_UPDATE", "/orders/" + orderId);
            }
        }
    }

    @RabbitListener(queues = "notification.queue")
    public void consumeFlashSaleStockEvent(FlashSaleStockEvent event) {
        log.info("Received FlashSaleStockEvent: flashSaleId={}, productId={}, sold={}", 
            event.getFlashSaleId(), event.getProductId(), event.getSoldQuantity());
        
        // Broadcast to all clients via WebSocket
        notificationService.broadcastFlashSaleUpdate(event);
    }
}
