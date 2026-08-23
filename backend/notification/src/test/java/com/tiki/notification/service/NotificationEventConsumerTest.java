package com.tiki.notification.service;

import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.common.event.FlashSaleStockEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    @DisplayName("consumeOrderCreatedEvent - sends notification")
    void consumeOrderCreatedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(100);
        event.setUserId(10);

        consumer.consumeOrderCreatedEvent(event);

        verify(notificationService).sendNotification(eq(10L), eq("Order Created"), anyString(), eq("ORDER_STATUS"), eq("/orders/100"));
    }

    @Test
    @DisplayName("consumeGenericNotification - sends generic notification")
    void consumeGenericNotification() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", 20);
        map.put("title", "Info");
        map.put("message", "Test MSG");

        consumer.consumeGenericNotification(map);

        verify(notificationService).sendNotification(eq(20L), eq("Info"), eq("Test MSG"), eq("SYSTEM"), eq("/"));
    }

    @Test
    @DisplayName("consumeStockEvent - sends order confirmed")
    void consumeStockEvent() {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", 100);
        map.put("status", "RESERVED");
        map.put("userId", 5);

        consumer.consumeStockEvent(map);

        verify(notificationService).sendNotification(eq(5L), eq("Order Confirmed"), anyString(), eq("ORDER_UPDATE"), eq("/orders/100"));
    }

    @Test
    @DisplayName("consumeFlashSaleStockEvent - broadcasts update")
    void consumeFlashSaleStockEvent() {
        FlashSaleStockEvent event = new FlashSaleStockEvent(1L, 10L, 100, 5);

        consumer.consumeFlashSaleStockEvent(event);

        verify(notificationService).broadcastFlashSaleUpdate(event);
    }
}
