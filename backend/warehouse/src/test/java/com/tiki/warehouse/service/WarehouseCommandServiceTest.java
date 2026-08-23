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

    @InjectMocks
    private WarehouseCommandService commandService;

    @Test
    @DisplayName("reserveStock - succeeds if sufficient stock")
    void reserveStock_Success() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));
        when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        boolean result = commandService.reserveStock(101L, 20);

        assertThat(result).isTrue();
        assertThat(entity.getReservedQuantity()).isEqualTo(30);
        verify(inventoryRepository).save(entity);
    }

    @Test
    @DisplayName("reserveStock - fails if insufficient stock")
    void reserveStock_FailsIfInsufficient() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(40)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));

        boolean result = commandService.reserveStock(101L, 20);

        assertThat(result).isFalse();
        assertThat(entity.getReservedQuantity()).isEqualTo(40); // unchanged
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
    @DisplayName("confirmOrder - reduces both quantity and reservedQuantity")
    void confirmOrder_ReducesQuantities() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));

        commandService.confirmOrder(101L, 5);

        assertThat(entity.getQuantity()).isEqualTo(45);
        assertThat(entity.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).save(entity);
    }
}
