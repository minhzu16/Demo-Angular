package com.tiki.order.service;

import com.tiki.order.entity.VoucherEntity;
import com.tiki.order.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VoucherConcurrencyTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private VoucherService voucherService;

    private static final String VOUCHER_CODE = "FLASH_SALE_5";
    private static final int MAX_USAGE = 5;
    private static final int CONCURRENT_THREADS = 30;

    private VoucherEntity mockVoucher;
    private AtomicInteger simulatedDbUsedCount;

    @BeforeEach
    void setUp() {
        simulatedDbUsedCount = new AtomicInteger(0);

        mockVoucher = new VoucherEntity();
        mockVoucher.setCode(VOUCHER_CODE);
        mockVoucher.setMaxUsage(MAX_USAGE);
        mockVoucher.setIsActive(true);

        // Giả lập chính xác hành vi của câu truy vấn Atomic SQL:
        // UPDATE vouchers SET used_count = used_count + 1 WHERE code = :code AND used_count < 5
        when(voucherRepository.tryApplyVoucher(eq(VOUCHER_CODE))).thenAnswer(inv -> {
            int current = simulatedDbUsedCount.get();
            while (current < MAX_USAGE) {
                if (simulatedDbUsedCount.compareAndSet(current, current + 1)) {
                    return 1; // Cập nhật thành công 1 hàng
                }
                current = simulatedDbUsedCount.get();
            }
            return 0; // Hết lượt (0 hàng cập nhật)
        });

        // Khi tryApplyVoucher trả về 0, service tra cứu voucher để đưa ra thông báo chính xác
        org.mockito.Mockito.lenient().when(voucherRepository.findByCodeIgnoreCase(eq(VOUCHER_CODE))).thenAnswer(inv -> {
            mockVoucher.setUsedCount(simulatedDbUsedCount.get());
            return Optional.of(mockVoucher);
        });
    }

    @Test
    @DisplayName("Kiểm thử tải đồng thời Voucher: 30 luồng tranh chấp voucher maxUsage=5 - Tuyệt đối không vượt quá giới hạn")
    void testConcurrentVoucherApplication_LimitsToMaxUsage() throws InterruptedException {
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
                    startLatch.await();
                    voucherService.applyVoucher(VOUCHER_CODE);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Kiểm chứng tính đúng đắn: Đúng 5 luồng áp dụng thành công và 25 luồng bị chặn
        assertThat(successCount.get()).isEqualTo(MAX_USAGE);
        assertThat(failCount.get()).isEqualTo(CONCURRENT_THREADS - MAX_USAGE);
        // Kiểm chứng số lượng đã dùng trong DB đúng bằng 5
        assertThat(simulatedDbUsedCount.get()).isEqualTo(MAX_USAGE);
    }
}
