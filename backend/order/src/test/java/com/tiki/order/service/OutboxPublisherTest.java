package com.tiki.order.service;

import com.tiki.order.entity.OutboxEventEntity;
import com.tiki.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    private OutboxEventEntity testEvent;

    @BeforeEach
    void setUp() {
        testEvent = OutboxEventEntity.builder()
                .id(1L)
                .aggregateType("ORDER")
                .aggregateId(100)
                .eventType("order.created")
                .exchange("tiki.events")
                .routingKey("order.created")
                .payload("{\"orderId\":100}")
                .status("PENDING")
                .retryCount(0)
                .build();
    }

    @Test
    @DisplayName("OutboxPublisher: Dispatch thành công và đánh dấu PUBLISHED")
    void publishPendingEvents_Success_MarksPublished() {
        when(outboxEventRepository.findPendingEventsForPublishing("PENDING", 5))
                .thenReturn(List.of(testEvent));

        outboxPublisher.publishPendingEvents();

        verify(rabbitTemplate).convertAndSend("tiki.events", "order.created", "{\"orderId\":100}");
        assertEquals("PUBLISHED", testEvent.getStatus());
        verify(outboxEventRepository).save(testEvent);
    }

    @Test
    @DisplayName("OutboxPublisher: Lỗi RabbitMQ tăng retryCount và giữ trạng thái")
    void publishPendingEvents_RabbitMqError_IncrementsRetryCount() {
        when(outboxEventRepository.findPendingEventsForPublishing("PENDING", 5))
                .thenReturn(List.of(testEvent));
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        outboxPublisher.publishPendingEvents();

        assertEquals(1, testEvent.getRetryCount());
        assertEquals("PENDING", testEvent.getStatus());
        verify(outboxEventRepository).save(testEvent);
    }

    @Test
    @DisplayName("OutboxPublisher: Đạt max retries chuyển sang FAILED")
    void publishPendingEvents_MaxRetriesReached_MarksFailed() {
        testEvent.setRetryCount(4); // 4 + 1 = 5 (>= MAX_RETRIES)
        when(outboxEventRepository.findPendingEventsForPublishing("PENDING", 5))
                .thenReturn(List.of(testEvent));
        doThrow(new RuntimeException("RabbitMQ dead"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        outboxPublisher.publishPendingEvents();

        assertEquals(5, testEvent.getRetryCount());
        assertEquals("FAILED", testEvent.getStatus());
        verify(outboxEventRepository).save(testEvent);
    }
}
