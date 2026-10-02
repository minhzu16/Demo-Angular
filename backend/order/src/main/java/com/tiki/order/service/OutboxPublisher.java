package com.tiki.order.service;

import com.tiki.order.entity.OutboxEventEntity;
import com.tiki.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_RETRIES = 5;

    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findPendingEventsForPublishing("PENDING", MAX_RETRIES);
        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    @Transactional
    public void publishSingleEvent(OutboxEventEntity event) {
        try {
            rabbitTemplate.convertAndSend(event.getExchange(), event.getRoutingKey(), event.getPayload());
            event.setStatus("PUBLISHED");
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
            log.info("Outbox event published successfully: id={}, type={}, aggregateId={}",
                    event.getId(), event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            int retries = event.getRetryCount() + 1;
            event.setRetryCount(retries);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            event.setErrorMessage(errorMsg);

            if (retries >= MAX_RETRIES) {
                event.setStatus("FAILED");
                log.error("Outbox event id={} permanently failed after {} retries: {}", event.getId(), retries, errorMsg);
            } else {
                log.warn("Outbox event id={} failed to publish (attempt {}/{}): {}", event.getId(), retries, MAX_RETRIES, errorMsg);
            }
            outboxEventRepository.save(event);
        }
    }
}
