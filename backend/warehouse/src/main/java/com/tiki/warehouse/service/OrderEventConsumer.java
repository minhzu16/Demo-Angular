package com.tiki.warehouse.service;

import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.common.event.OrderItemDto;
import com.tiki.warehouse.config.WarehouseRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final WarehouseCommandService warehouseCommandService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = WarehouseRabbitMQConfig.WAREHOUSE_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for order id: {}", event.getOrderId());
        
        boolean success = true; 
        
        if (event.getItems() != null) {
            for (OrderItemDto item : event.getItems()) {
                boolean reserved = warehouseCommandService.reserveStock(item.getProductId().longValue(), item.getQuantity());
                if (!reserved) {
                    success = false;
                    break;
                }
            }
        }
        
        if (success) {
            log.info("Stock reserved successfully for order {}", event.getOrderId());
            rabbitTemplate.convertAndSend(WarehouseRabbitMQConfig.WAREHOUSE_EVENTS_EXCHANGE, 
                                         WarehouseRabbitMQConfig.STOCK_RESERVED_ROUTING_KEY, 
                                         Map.of("orderId", event.getOrderId(), "status", "RESERVED"));
        } else {
            log.warn("Stock reservation failed for order {}", event.getOrderId());
            rabbitTemplate.convertAndSend(WarehouseRabbitMQConfig.WAREHOUSE_EVENTS_EXCHANGE, 
                                         WarehouseRabbitMQConfig.STOCK_FAILED_ROUTING_KEY, 
                                         Map.of("orderId", event.getOrderId(), "status", "FAILED", "reason", "Insufficient stock"));
        }
    }
}
