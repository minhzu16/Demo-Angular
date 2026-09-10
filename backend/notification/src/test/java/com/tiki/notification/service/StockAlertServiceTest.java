package com.tiki.notification.service;

import com.tiki.notification.entity.StockAlertEntity;
import com.tiki.notification.repository.StockAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private StockAlertRepository stockAlertRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StockAlertService stockAlertService;

    @Test
    @DisplayName("subscribe creates new StockAlertEntity")
    void testSubscribeNew() {
        when(stockAlertRepository.existsByProductIdAndUserIdAndNotifiedFalse(101L, 1L)).thenReturn(false);

        StockAlertEntity saved = StockAlertEntity.builder()
                .id(1L)
                .userId(1L)
                .productId(101L)
                .userEmail("test@example.com")
                .notified(false)
                .build();

        when(stockAlertRepository.save(any(StockAlertEntity.class))).thenReturn(saved);

        StockAlertEntity result = stockAlertService.subscribe(1L, "test@example.com", 101L);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(101L);
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(stockAlertRepository).save(any());
    }

    @Test
    @DisplayName("subscribe with invalid productId throws IllegalArgumentException")
    void testSubscribeInvalidProduct() {
        assertThatThrownBy(() -> stockAlertService.subscribe(1L, "test@example.com", null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stockAlertService.subscribe(1L, "test@example.com", -5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("triggerRestockAlert sends notification and marks subscribers as notified")
    void testTriggerRestockAlert() {
        StockAlertEntity sub1 = StockAlertEntity.builder()
                .id(10L)
                .userId(1L)
                .productId(101L)
                .notified(false)
                .build();

        StockAlertEntity sub2 = StockAlertEntity.builder()
                .id(11L)
                .userId(2L)
                .productId(101L)
                .notified(false)
                .build();

        when(stockAlertRepository.findByProductIdAndNotifiedFalse(101L)).thenReturn(List.of(sub1, sub2));

        int notifiedCount = stockAlertService.triggerRestockAlert(101L);

        assertThat(notifiedCount).isEqualTo(2);
        assertThat(sub1.isNotified()).isTrue();
        assertThat(sub1.getNotifiedAt()).isNotNull();
        assertThat(sub2.isNotified()).isTrue();

        verify(notificationService, times(2)).sendNotification(anyLong(), anyString(), anyString(), eq("STOCK_ALERT"), anyString());
        verify(stockAlertRepository).saveAll(any());
    }
}
