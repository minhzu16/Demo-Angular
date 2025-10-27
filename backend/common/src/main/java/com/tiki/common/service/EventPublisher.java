package com.tiki.common.service;

import com.tiki.common.config.RabbitMQConfig;
import com.tiki.common.event.NotificationEvent;
import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.common.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Event Publisher Service
 * Sprint 9 - Day 7-9
 * 
 * Publishes events to RabbitMQ for async processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * Publish Order Created Event
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("Publishing order created event: orderId={}", event.getOrderId());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_KEY,
                event
            );
            log.info("Order created event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish order created event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    /**
     * Publish Product Updated Event
     */
    public void publishProductUpdated(ProductUpdatedEvent event) {
        try {
            log.info("Publishing product updated event: productId={}, action={}", 
                event.getProductId(), event.getAction());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRODUCT_EXCHANGE,
                RabbitMQConfig.PRODUCT_UPDATED_KEY,
                event
            );
            log.info("Product updated event published successfully");
        } catch (Exception e) {
            log.error("Failed to publish product updated event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    /**
     * Publish Email Notification Event
     */
    public void publishEmailNotification(NotificationEvent event) {
        try {
            log.info("Publishing email notification: recipient={}", event.getRecipient());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_EMAIL_KEY,
                event
            );
            log.info("Email notification published successfully");
        } catch (Exception e) {
            log.error("Failed to publish email notification", e);
            // Don't throw exception for notifications - they're not critical
        }
    }
    
    /**
     * Publish SMS Notification Event
     */
    public void publishSmsNotification(NotificationEvent event) {
        try {
            log.info("Publishing SMS notification: recipient={}", event.getRecipient());
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_SMS_KEY,
                event
            );
            log.info("SMS notification published successfully");
        } catch (Exception e) {
            log.error("Failed to publish SMS notification", e);
            // Don't throw exception for notifications - they're not critical
        }
    }
}
