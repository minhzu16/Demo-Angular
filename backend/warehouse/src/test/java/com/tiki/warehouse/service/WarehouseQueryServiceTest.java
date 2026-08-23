package com.tiki.warehouse.service;

import com.tiki.warehouse.dto.InventoryDto;
import com.tiki.warehouse.entity.InventoryEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseQueryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Spy
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private WarehouseQueryService queryService;

    @Test
    @DisplayName("getStock - returns correct available stock")
    void getStock_ReturnsAvailableStock() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .reservedQuantity(10)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));

        Integer stock = queryService.getStock(101L);

        assertThat(stock).isEqualTo(40);
    }

    @Test
    @DisplayName("getStock - returns 0 if product not found")
    void getStock_ReturnsZeroIfNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        Integer stock = queryService.getStock(999L);

        assertThat(stock).isZero();
    }

    @Test
    @DisplayName("getInventory - returns DTO if product exists")
    void getInventory_ReturnsDto() {
        InventoryEntity entity = InventoryEntity.builder()
                .productId(101L)
                .quantity(50)
                .build();
        when(inventoryRepository.findByProductId(101L)).thenReturn(Optional.of(entity));

        InventoryDto result = queryService.getInventory(101L);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(101L);
        assertThat(result.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("getShopStats - calculates stats correctly")
    void getShopStats_CalculatesStats() {
        InventoryEntity item1 = InventoryEntity.builder().productId(1L).quantity(50).build();
        InventoryEntity item2 = InventoryEntity.builder().productId(2L).quantity(5).build();
        InventoryEntity item3 = InventoryEntity.builder().productId(3L).quantity(0).build();

        when(inventoryRepository.findByShopId(10L)).thenReturn(List.of(item1, item2, item3));

        Map<String, Object> stats = queryService.getShopStats(10L);

        assertThat(stats.get("totalProducts")).isEqualTo(3L);
        assertThat(stats.get("lowStockProducts")).isEqualTo(1L);
        assertThat(stats.get("outOfStockProducts")).isEqualTo(1L);
    }
}
