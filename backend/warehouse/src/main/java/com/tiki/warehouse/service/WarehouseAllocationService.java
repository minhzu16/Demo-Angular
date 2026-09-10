package com.tiki.warehouse.service;

import com.tiki.warehouse.dto.*;
import com.tiki.warehouse.entity.WarehouseLocationEntity;
import com.tiki.warehouse.entity.WarehouseStockEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import com.tiki.warehouse.repository.WarehouseLocationRepository;
import com.tiki.warehouse.repository.WarehouseStockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseAllocationService {

    private final WarehouseLocationRepository locationRepository;
    private final WarehouseStockRepository stockRepository;
    private final InventoryRepository inventoryRepository;

    private static final Set<String> NORTHERN_PROVINCES = Set.of(
            "hà nội", "ha noi", "hải phòng", "hai phong", "quảng ninh", "quang ninh",
            "bắc ninh", "bac ninh", "hải dương", "hai duong", "hưng yên", "hung yen",
            "thái bình", "thai binh", "nam định", "nam dinh", "ninh bình", "ninh binh",
            "vĩnh phúc", "vinh phuc", "phú thọ", "phu tho", "thái nguyên", "thai nguyen",
            "bắc giang", "bac giang", "lạng sơn", "lang son", "hà nam", "ha nam"
    );

    private static final Set<String> CENTRAL_PROVINCES = Set.of(
            "đà nẵng", "da nang", "quảng nam", "quang nam", "quảng ngãi", "quang ngai",
            "bình định", "binh dinh", "phú yên", "phu yen", "khánh hòa", "khanh hoa",
            "thừa thiên huế", "thua thien hue", "quảng trị", "quang tri", "quảng bình", "quang binh",
            "hà tĩnh", "ha tinh", "nghệ an", "nghe an", "thanh hóa", "thanh hoa"
    );

    @PostConstruct
    public void initDefaultWarehouses() {
        try {
            if (locationRepository.count() == 0) {
                log.info("Seeding default regional warehouses...");
                locationRepository.save(WarehouseLocationEntity.builder()
                        .code("WH-HN-01")
                        .name("Kho Tổng Miền Bắc (Hà Nội)")
                        .province("Hà Nội")
                        .address("Khu CN Đài Tư, Quận Long Biên, TP. Hà Nội")
                        .capacity(50000)
                        .isActive(true)
                        .build());

                locationRepository.save(WarehouseLocationEntity.builder()
                        .code("WH-DN-01")
                        .name("Kho Tổng Miền Trung (Đà Nẵng)")
                        .province("Đà Nẵng")
                        .address("Khu CN Hòa Khánh, Quận Liên Chiểu, TP. Đà Nẵng")
                        .capacity(30000)
                        .isActive(true)
                        .build());

                locationRepository.save(WarehouseLocationEntity.builder()
                        .code("WH-HCM-01")
                        .name("Kho Tổng Miền Nam (Hồ Chí Minh)")
                        .province("Hồ Chí Minh")
                        .address("Khu CN Tân Bình, Quận Tân Phú, TP. Hồ Chí Minh")
                        .capacity(80000)
                        .isActive(true)
                        .build());
                log.info("Seeded 3 default regional warehouses successfully.");
            }
        } catch (Exception e) {
            log.warn("Could not seed default warehouses: {}", e.getMessage());
        }
    }

    public List<WarehouseLocationDto> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseLocationDto createLocation(CreateWarehouseLocationRequest request) {
        if (locationRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã kho " + request.getCode() + " đã tồn tại.");
        }

        WarehouseLocationEntity entity = WarehouseLocationEntity.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .province(request.getProvince().trim())
                .address(request.getAddress())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 10000)
                .isActive(true)
                .build();

        return toDto(locationRepository.save(entity));
    }

    public List<WarehouseStockEntity> getStockByWarehouse(Long productId) {
        return stockRepository.findByProductId(productId);
    }

    @Transactional
    public void updateWarehouseStock(Long warehouseId, Long productId, Integer quantity) {
        WarehouseStockEntity stock = stockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> WarehouseStockEntity.builder()
                        .warehouseId(warehouseId)
                        .productId(productId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        stock.setQuantity(quantity != null ? Math.max(0, quantity) : 0);
        stock.setUpdatedAt(LocalDateTime.now());
        stockRepository.save(stock);
        log.info("Updated warehouse stock: warehouseId={}, productId={}, quantity={}", warehouseId, productId, quantity);
    }

    @Transactional
    public WarehouseAllocationResponse allocateFulfillment(WarehouseAllocationRequest request) {
        log.info("Allocating fulfillment: destinationProvince={}, itemCount={}",
                request.getDestinationProvince(), request.getItems() != null ? request.getItems().size() : 0);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return WarehouseAllocationResponse.builder()
                    .success(false)
                    .message("Không có sản phẩm nào trong yêu cầu phân bổ.")
                    .build();
        }

        List<WarehouseLocationEntity> activeWarehouses = locationRepository.findByIsActiveTrue();
        if (activeWarehouses.isEmpty()) {
            return WarehouseAllocationResponse.builder()
                    .success(false)
                    .message("Hệ thống hiện không có kho hàng nào đang hoạt động.")
                    .build();
        }

        // Sort warehouses by geographic proximity to destination
        List<WarehouseLocationEntity> prioritizedWarehouses = prioritizeWarehouses(activeWarehouses, request.getDestinationProvince());

        // Find first warehouse that can fulfill all items
        for (WarehouseLocationEntity warehouse : prioritizedWarehouses) {
            boolean canFulfillAll = true;
            for (WarehouseAllocationRequest.AllocationItem item : request.getItems()) {
                Optional<WarehouseStockEntity> stockOpt = stockRepository.findByWarehouseIdAndProductId(warehouse.getId(), item.getProductId());
                if (stockOpt.isEmpty() || stockOpt.get().getAvailableQuantity() < item.getQuantity()) {
                    canFulfillAll = false;
                    break;
                }
            }

            if (canFulfillAll) {
                // Reserve stock in this warehouse
                List<WarehouseAllocationResponse.ItemAllocationDetail> itemDetails = new ArrayList<>();
                for (WarehouseAllocationRequest.AllocationItem item : request.getItems()) {
                    int reserved = stockRepository.tryReserveWarehouseStock(warehouse.getId(), item.getProductId(), item.getQuantity());
                    inventoryRepository.tryReserveStock(item.getProductId(), item.getQuantity());

                    itemDetails.add(WarehouseAllocationResponse.ItemAllocationDetail.builder()
                            .productId(item.getProductId())
                            .requestedQuantity(item.getQuantity())
                            .allocatedQuantity(item.getQuantity())
                            .reserved(reserved > 0)
                            .build());
                }

                log.info("Successfully allocated order to warehouse: {} ({})", warehouse.getName(), warehouse.getCode());
                return WarehouseAllocationResponse.builder()
                        .success(true)
                        .allocatedWarehouseId(warehouse.getId())
                        .allocatedWarehouseCode(warehouse.getCode())
                        .allocatedWarehouseName(warehouse.getName())
                        .message("Phân bổ kho thành công.")
                        .items(itemDetails)
                        .build();
            }
        }

        log.warn("No single warehouse has sufficient stock for all items in province: {}", request.getDestinationProvince());
        return WarehouseAllocationResponse.builder()
                .success(false)
                .message("Không có kho hàng nào còn đủ tồn kho đáp ứng toàn bộ đơn hàng.")
                .build();
    }

    private List<WarehouseLocationEntity> prioritizeWarehouses(List<WarehouseLocationEntity> list, String province) {
        if (province == null || province.isBlank()) {
            return list;
        }

        String normalized = province.trim().toLowerCase();
        String targetCode;

        if (NORTHERN_PROVINCES.contains(normalized)) {
            targetCode = "WH-HN-01";
        } else if (CENTRAL_PROVINCES.contains(normalized)) {
            targetCode = "WH-DN-01";
        } else {
            targetCode = "WH-HCM-01";
        }

        List<WarehouseLocationEntity> result = new ArrayList<>(list);
        result.sort((w1, w2) -> {
            if (targetCode.equalsIgnoreCase(w1.getCode())) return -1;
            if (targetCode.equalsIgnoreCase(w2.getCode())) return 1;
            return 0;
        });
        return result;
    }

    private WarehouseLocationDto toDto(WarehouseLocationEntity entity) {
        return WarehouseLocationDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .province(entity.getProvince())
                .address(entity.getAddress())
                .capacity(entity.getCapacity())
                .isActive(entity.getIsActive())
                .build();
    }
}
