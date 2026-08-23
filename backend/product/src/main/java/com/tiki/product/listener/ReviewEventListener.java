package com.tiki.product.listener;

import com.tiki.common.config.RabbitMQConfig;
import com.tiki.common.event.ReviewCreatedEvent;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.repository.ProductRepository;
import com.tiki.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Review Event Listener
 * 
 * Listens to review events and updates product statistics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final ProductRepository productRepository;
    private final ProductSearchService searchService;

    /**
     * Handle Review Created Event
     * Updates average rating and review count for the product
     */
    @RabbitListener(queues = RabbitMQConfig.REVIEW_CREATED_QUEUE)
    @Transactional
    public void handleReviewCreated(ReviewCreatedEvent event) {
        log.info("Received review created event for product {}: rating={}", 
            event.getProductId(), event.getRating());
        
        productRepository.findById(event.getProductId().intValue()).ifPresent(product -> {
            int oldCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
            double oldRating = product.getAverageRating() != null ? product.getAverageRating() : 0.0;
            
            int newCount = oldCount + 1;
            double newRating = ((oldRating * oldCount) + event.getRating()) / newCount;
            
            product.setReviewCount(newCount);
            product.setAverageRating(newRating);
            
            productRepository.save(product);
            
            // Sync with Elasticsearch
            try {
                searchService.indexProduct(product);
                log.info("Product {} reindexed in Elasticsearch after new review", product.getId());
            } catch (Exception e) {
                log.error("Failed to reindex product {} after review: {}", product.getId(), e.getMessage());
            }

            log.info("Updated product {} stats: count={}, rating={}", 
                product.getId(), newCount, String.format("%.2f", newRating));
        });
    }
    /**
     * Handle Review Deleted Event
     * Updates average rating and review count for the product
     */
    @RabbitListener(queues = RabbitMQConfig.REVIEW_DELETED_QUEUE)
    @Transactional
    public void handleReviewDeleted(com.tiki.common.event.ReviewDeletedEvent event) {
        log.info("Received review deleted event for product {}: rating={}", 
            event.getProductId(), event.getRating());
        
        productRepository.findById(event.getProductId().intValue()).ifPresent(product -> {
            int oldCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
            double oldRating = product.getAverageRating() != null ? product.getAverageRating() : 0.0;
            
            if (oldCount > 1) {
                int newCount = oldCount - 1;
                // Reverse the average calculation: (oldAvg * oldCount - deletedRating) / newCount
                double newRating = ((oldRating * oldCount) - event.getRating()) / newCount;
                if (newRating < 0) newRating = 0;
                if (newRating > 5) newRating = 5;
                
                product.setReviewCount(newCount);
                product.setAverageRating(newRating);
            } else {
                product.setReviewCount(0);
                product.setAverageRating(0.0);
            }
            
            productRepository.save(product);
            
            // Sync with Elasticsearch
            try {
                searchService.indexProduct(product);
                log.info("Product {} reindexed in Elasticsearch after review deletion", product.getId());
            } catch (Exception e) {
                log.error("Failed to reindex product {} after review deletion: {}", product.getId(), e.getMessage());
            }

            log.info("Updated product {} stats after deletion: count={}, rating={}", 
                product.getId(), product.getReviewCount(), String.format("%.2f", product.getAverageRating()));
        });
    }
}
