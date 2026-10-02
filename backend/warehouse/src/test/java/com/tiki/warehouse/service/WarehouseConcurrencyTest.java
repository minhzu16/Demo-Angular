package com.tiki.warehouse.service;

import com.tiki.warehouse.entity.InventoryEntity;
import com.tiki.warehouse.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class WarehouseConcurrencyTest {

    @Autowired
    private WarehouseCommandService warehouseCommandService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.tiki.warehouse.client.NotificationClient notificationClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.tiki.warehouse.service.OrderEventConsumer orderEventConsumer;

    private static final Long TEST_PRODUCT_ID = 9999L;
    private static final int INITIAL_STOCK = 10;
    private static final int CONCURRENT_THREADS = 50;

    @BeforeEach
    void setUp() {
        inventoryRepository.findByProductId(TEST_PRODUCT_ID).ifPresent(inventoryRepository::delete);
        
        InventoryEntity initialInventory = InventoryEntity.builder()
                .productId(TEST_PRODUCT_ID)
                .shopId(1L)
                .quantity(INITIAL_STOCK)
                .reservedQuantity(0)
                .build();
        inventoryRepository.saveAndFlush(initialInventory);
    }

    @Test
    @DisplayName("Kiểm thử tải đồng thời: 50 luồng tranh chấp 10 sản phẩm tồn kho - Chống Overselling tuyệt đối")
    void testConcurrentStockReservation_PreventsOverselling() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // Chờ tất cả 50 luồng sẵn sàng rồi cùng ập vào đồng thời
                    startLatch.await();
                    boolean reserved = warehouseCommandService.reserveStock(TEST_PRODUCT_ID, 1);
                    if (reserved) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Chờ tất cả luồng đã sẵn sàng ở vạch xuất phát
        readyLatch.await(5, TimeUnit.SECONDS);
        // Bắn súng lệnh xuất phát đồng thời 50 luồng
        startLatch.countDown();
        // Chờ tất cả luồng hoàn thành (tối đa 15 giây)
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Kiểm chứng tính đúng đắn toán học: Đúng 10 đơn thành công và 40 đơn thất bại
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(failCount.get()).isEqualTo(CONCURRENT_THREADS - INITIAL_STOCK);

        // Kiểm chứng trạng thái kho trong Database: Không được phép âm kho (zero overselling)
        InventoryEntity finalInventory = inventoryRepository.findByProductId(TEST_PRODUCT_ID).orElseThrow();
        assertThat(finalInventory.getReservedQuantity()).isEqualTo(INITIAL_STOCK);
        int availableStock = finalInventory.getQuantity() - finalInventory.getReservedQuantity();
        assertThat(availableStock).isEqualTo(0);
    }
}
