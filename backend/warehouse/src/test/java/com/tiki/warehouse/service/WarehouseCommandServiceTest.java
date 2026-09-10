package com.tiki.warehouse.service;

import com.tiki.warehouse.entity.InventoryEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseCommandServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private com.tiki.warehouse.client.NotificationClient notificationClient;

    @InjectMocks
    private WarehouseCommandService commandService;

    @Test
    @DisplayName("reserveStock - atomic query succeeds if sufficient stock")
    void reserveStock_Success() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));
        when(inventoryRepository.tryReserveStock(101L, 20)).thenReturn(1);

        boolean result = commandService.reserveStock(101L, 20);

        assertThat(result).isTrue();
        verify(inventoryRepository).tryReserveStock(101L, 20);
    }

    @Test
    @DisplayName("reserveStock - atomic query fails and returns false if insufficient stock")
    void reserveStock_FailsIfInsufficient() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(40)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));
        when(inventoryRepository.tryReserveStock(101L, 20)).thenReturn(0);

        boolean result = commandService.reserveStock(101L, 20);

        assertThat(result).isFalse();
        verify(inventoryRepository).tryReserveStock(101L, 20);
    }

    @Test
    @DisplayName("updateStock - updates quantity correctly")
    void updateStock_UpdatesQuantity() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));

        commandService.updateStock(101L, 100);

        assertThat(entity.getQuantity()).isEqualTo(100);
        verify(inventoryRepository).save(entity);
    }

    @Test
    @DisplayName("updateStock - triggers restock alert when quantity goes from 0 to positive")
    void updateStock_TriggersRestockAlert_WhenRestockedFromZero() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));

        commandService.updateStock(101L, 20);

        assertThat(entity.getQuantity()).isEqualTo(20);
        verify(inventoryRepository).save(entity);
        verify(notificationClient).triggerRestockAlert(101L);
    }

    @Test
    @DisplayName("confirmOrder - calls atomic confirmStock")
    void confirmOrder_CallsAtomicConfirm() {
        when(inventoryRepository.confirmStock(101L, 5)).thenReturn(1);

        commandService.confirmOrder(101L, 5);

        verify(inventoryRepository).confirmStock(101L, 5);
    }

    @Test
    @DisplayName("releaseStock - calls atomic releaseReservedStock")
    void releaseStock_CallsAtomicRelease() {
        when(inventoryRepository.releaseReservedStock(101L, 5)).thenReturn(1);

        commandService.releaseStock(101L, 5);

        verify(inventoryRepository).releaseReservedStock(101L, 5);
    }
}
