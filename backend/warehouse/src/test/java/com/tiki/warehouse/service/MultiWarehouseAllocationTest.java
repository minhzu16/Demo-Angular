package com.tiki.warehouse.service;

import com.tiki.warehouse.dto.CreateWarehouseLocationRequest;
import com.tiki.warehouse.dto.WarehouseAllocationRequest;
import com.tiki.warehouse.dto.WarehouseAllocationResponse;
import com.tiki.warehouse.dto.WarehouseLocationDto;
import com.tiki.warehouse.entity.WarehouseLocationEntity;
import com.tiki.warehouse.entity.WarehouseStockEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import com.tiki.warehouse.repository.WarehouseLocationRepository;
import com.tiki.warehouse.repository.WarehouseStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiWarehouseAllocationTest {

    @Mock
    private WarehouseLocationRepository locationRepository;

    @Mock
    private WarehouseStockRepository stockRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private WarehouseAllocationService allocationService;

    private WarehouseLocationEntity whHn;
    private WarehouseLocationEntity whHcm;
    private WarehouseLocationEntity whDn;

    @BeforeEach
    void setUp() {
        whHn = WarehouseLocationEntity.builder()
                .id(1L)
                .code("WH-HN-01")
                .name("Kho Hà Nội")
                .province("Hà Nội")
                .isActive(true)
                .build();

        whDn = WarehouseLocationEntity.builder()
                .id(2L)
                .code("WH-DN-01")
                .name("Kho Đà Nẵng")
                .province("Đà Nẵng")
                .isActive(true)
                .build();

        whHcm = WarehouseLocationEntity.builder()
                .id(3L)
                .code("WH-HCM-01")
                .name("Kho Hồ Chí Minh")
                .province("Hồ Chí Minh")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("allocateFulfillment - Buyer in Hà Nội prefers WH-HN-01 when stock available")
    void testAllocateFulfillment_NorthernCustomer() {
        when(locationRepository.findByIsActiveTrue()).thenReturn(List.of(whHn, whDn, whHcm));

        WarehouseStockEntity stockHn = WarehouseStockEntity.builder()
                .warehouseId(1L)
                .productId(101L)
                .quantity(50)
                .reservedQuantity(5)
                .build();

        when(stockRepository.findByWarehouseIdAndProductId(1L, 101L)).thenReturn(Optional.of(stockHn));
        when(stockRepository.tryReserveWarehouseStock(1L, 101L, 2)).thenReturn(1);
        when(inventoryRepository.tryReserveStock(101L, 2)).thenReturn(1);

        WarehouseAllocationRequest request = WarehouseAllocationRequest.builder()
                .destinationProvince("Hà Nội")
                .items(List.of(WarehouseAllocationRequest.AllocationItem.builder()
                        .productId(101L)
                        .quantity(2)
                        .build()))
                .build();

        WarehouseAllocationResponse response = allocationService.allocateFulfillment(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAllocatedWarehouseCode()).isEqualTo("WH-HN-01");
        assertThat(response.getAllocatedWarehouseId()).isEqualTo(1L);
        verify(stockRepository).tryReserveWarehouseStock(1L, 101L, 2);
    }

    @Test
    @DisplayName("allocateFulfillment - Buyer in Cần Thơ prefers WH-HCM-01 when stock available")
    void testAllocateFulfillment_SouthernCustomer() {
        when(locationRepository.findByIsActiveTrue()).thenReturn(List.of(whHn, whDn, whHcm));

        WarehouseStockEntity stockHcm = WarehouseStockEntity.builder()
                .warehouseId(3L)
                .productId(202L)
                .quantity(100)
                .reservedQuantity(0)
                .build();

        when(stockRepository.findByWarehouseIdAndProductId(3L, 202L)).thenReturn(Optional.of(stockHcm));
        when(stockRepository.tryReserveWarehouseStock(3L, 202L, 5)).thenReturn(1);

        WarehouseAllocationRequest request = WarehouseAllocationRequest.builder()
                .destinationProvince("Cần Thơ")
                .items(List.of(WarehouseAllocationRequest.AllocationItem.builder()
                        .productId(202L)
                        .quantity(5)
                        .build()))
                .build();

        WarehouseAllocationResponse response = allocationService.allocateFulfillment(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAllocatedWarehouseCode()).isEqualTo("WH-HCM-01");
        assertThat(response.getAllocatedWarehouseId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("allocateFulfillment - Falls back to HCM when nearest HN warehouse is out of stock")
    void testAllocateFulfillment_FallbackWhenNearestOutOfStock() {
        when(locationRepository.findByIsActiveTrue()).thenReturn(List.of(whHn, whDn, whHcm));

        // HN has 0 stock
        WarehouseStockEntity stockHn = WarehouseStockEntity.builder()
                .warehouseId(1L)
                .productId(101L)
                .quantity(1)
                .reservedQuantity(1) // available = 0
                .build();

        // DN has 0 stock
        WarehouseStockEntity stockDn = WarehouseStockEntity.builder()
                .warehouseId(2L)
                .productId(101L)
                .quantity(0)
                .reservedQuantity(0)
                .build();

        // HCM has 20 stock
        WarehouseStockEntity stockHcm = WarehouseStockEntity.builder()
                .warehouseId(3L)
                .productId(101L)
                .quantity(20)
                .reservedQuantity(0)
                .build();

        when(stockRepository.findByWarehouseIdAndProductId(1L, 101L)).thenReturn(Optional.of(stockHn));
        when(stockRepository.findByWarehouseIdAndProductId(2L, 101L)).thenReturn(Optional.of(stockDn));
        when(stockRepository.findByWarehouseIdAndProductId(3L, 101L)).thenReturn(Optional.of(stockHcm));
        when(stockRepository.tryReserveWarehouseStock(3L, 101L, 5)).thenReturn(1);

        WarehouseAllocationRequest request = WarehouseAllocationRequest.builder()
                .destinationProvince("Hải Phòng") // Northern province
                .items(List.of(WarehouseAllocationRequest.AllocationItem.builder()
                        .productId(101L)
                        .quantity(5)
                        .build()))
                .build();

        WarehouseAllocationResponse response = allocationService.allocateFulfillment(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAllocatedWarehouseCode()).isEqualTo("WH-HCM-01");
        verify(stockRepository).tryReserveWarehouseStock(3L, 101L, 5);
    }

    @Test
    @DisplayName("allocateFulfillment - Fails when no warehouse has sufficient stock")
    void testAllocateFulfillment_FailsWhenInsufficientStock() {
        when(locationRepository.findByIsActiveTrue()).thenReturn(List.of(whHn, whDn, whHcm));

        when(stockRepository.findByWarehouseIdAndProductId(any(), eq(999L))).thenReturn(Optional.empty());

        WarehouseAllocationRequest request = WarehouseAllocationRequest.builder()
                .destinationProvince("Hà Nội")
                .items(List.of(WarehouseAllocationRequest.AllocationItem.builder()
                        .productId(999L)
                        .quantity(10)
                        .build()))
                .build();

        WarehouseAllocationResponse response = allocationService.allocateFulfillment(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Không có kho hàng nào");
    }

    @Test
    @DisplayName("createLocation - Successfully creates and saves new warehouse location")
    void testCreateLocation_Success() {
        CreateWarehouseLocationRequest request = CreateWarehouseLocationRequest.builder()
                .code("WH-HP-01")
                .name("Kho Hải Phòng")
                .province("Hải Phòng")
                .address("Khu CN Đình Vũ, Hải Phòng")
                .capacity(20000)
                .build();

        when(locationRepository.existsByCode("WH-HP-01")).thenReturn(false);
        when(locationRepository.save(any(WarehouseLocationEntity.class))).thenAnswer(i -> {
            WarehouseLocationEntity e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        WarehouseLocationDto result = allocationService.createLocation(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCode()).isEqualTo("WH-HP-01");
        assertThat(result.getName()).isEqualTo("Kho Hải Phòng");
    }

    @Test
    @DisplayName("createLocation - Throws exception when warehouse code already exists")
    void testCreateLocation_DuplicateCode() {
        CreateWarehouseLocationRequest request = CreateWarehouseLocationRequest.builder()
                .code("WH-HN-01")
                .name("Kho Trùng Lặp")
                .province("Hà Nội")
                .build();

        when(locationRepository.existsByCode("WH-HN-01")).thenReturn(true);

        assertThatThrownBy(() -> allocationService.createLocation(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại");
    }
}
