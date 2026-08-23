package com.tiki.product.listener;

import com.tiki.common.config.RabbitMQConfig;
import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.common.event.OrderItemDto;
import com.tiki.product.entity.FlashSale;
import com.tiki.product.entity.FlashSaleProduct;
import com.tiki.product.repository.FlashSaleProductRepository;
import com.tiki.product.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tiki.common.event.FlashSaleStockEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flash Sale Event Listener
 * 
 * Listens to order created events and updates flash sale statistics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleEventListener {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Handle Order Created Event
     * Decrements available quantity for flash sale products
     */
    @RabbitListener(queues = RabbitMQConfig.PRODUCT_ORDER_QUEUE) 
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("FlashSaleListener: Processing order {}", event.getOrderId());
        
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("FlashSaleListener: No items in order {}", event.getOrderId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> activeSales = flashSaleRepository.findActiveFlashSales(now);
        
        if (activeSales.isEmpty()) {
            log.info("FlashSaleListener: No active flash sales at the moment");
            return;
        }

        for (OrderItemDto item : event.getItems()) {
            for (FlashSale sale : activeSales) {
                flashSaleProductRepository.findByFlashSaleId(sale.getId()).stream()
                    .filter(fp -> fp.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .ifPresent(fp -> {
                        int quantityToAdd = item.getQuantity();
                        log.info("FlashSaleListener: Incrementing sold quantity for product {} in flash sale {}: +{}", 
                            item.getProductId(), sale.getName(), quantityToAdd);
                        
                        fp.setQuantitySold(fp.getQuantitySold() + quantityToAdd);
                        FlashSaleProduct saved = flashSaleProductRepository.save(fp);
                        
                        // Broadcast stock update
                        FlashSaleStockEvent stockEvent = FlashSaleStockEvent.builder()
                            .flashSaleId(sale.getId())
                            .productId(fp.getProductId())
                            .soldQuantity(saved.getQuantitySold())
                            .totalQuantity(saved.getQuantityLimit())
                            .build();
                        
                        rabbitTemplate.convertAndSend("tiki.events", "flashsale.stock.update", stockEvent);
                    });
            }
        }
    }
}
