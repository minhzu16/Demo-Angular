package com.tiki.order.service;

import com.tiki.order.config.RabbitMQOrderConfig;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import com.tiki.order.entity.OrderTrackingEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderTrackingRepository orderTrackingRepository;

    @RabbitListener(queues = RabbitMQOrderConfig.STOCK_QUEUE)
    @Transactional
    public void handleStockEvent(Map<String, Object> message) {
        Integer orderId = (Integer) message.get("orderId");
        String status = (String) message.get("status");
        
        log.info("Received stock event for order {}: status {}", orderId, status);
        
        orderRepository.findById(orderId).ifPresent(order -> {
            if ("RESERVED".equals(status)) {
                order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
                orderTrackingRepository.save(new OrderTrackingEntity(orderId, order.getStatus(), "Kho đã xác nhận và giữ hàng"));
            } else {
                order.setStatus(OrderEntity.OrderStatus.CANCELLED);
                String reason = (String) message.getOrDefault("reason", "Kho hết hàng");
                orderTrackingRepository.save(new OrderTrackingEntity(orderId, order.getStatus(), "Đơn hàng bị hủy do: " + reason));
            }
            orderRepository.save(order);
        });
    }
}
