package com.tiki.analytics.service;

import com.tiki.analytics.client.OrderClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsServiceTest {

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private SalesAnalyticsService salesAnalyticsService;

    @Test
    @DisplayName("Sales Overview - Tính toán đúng tổng doanh thu, số đơn và AOV từ dữ liệu orderClient thực tế")
    void testSalesOverview_RealData() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 2);

        when(orderClient.getRevenueStats(eq("2026-09-01"), eq("2026-09-02"), anyString(), anyString()))
                .thenReturn(List.of(
                        Map.of("date", "2026-09-01", "revenue", "1000000", "orderCount", 5),
                        Map.of("date", "2026-09-02", "revenue", "2000000", "orderCount", 5)
                ));

        Map<String, Object> overview = salesAnalyticsService.getSalesOverview(100L, start, end);

        assertNotNull(overview);
        assertEquals(new BigDecimal("3000000"), overview.get("totalRevenue"));
        assertEquals(10, overview.get("totalOrders"));
        // AOV = 3,000,000 / 10 = 300,000.00
        assertEquals(new BigDecimal("300000.00"), overview.get("averageOrderValue"));
    }

    @Test
    @DisplayName("Sales By Range - Trả về dữ liệu chi tiết theo từng ngày và tổng kết")
    void testSalesByRange_Summary() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 3);

        when(orderClient.getRevenueStats(eq("2026-09-01"), eq("2026-09-03"), anyString(), anyString()))
                .thenReturn(List.of(
                        Map.of("date", "2026-09-01", "revenue", "500000", "orderCount", 2),
                        Map.of("date", "2026-09-02", "revenue", "600000", "orderCount", 3)
                ));

        Map<String, Object> range = salesAnalyticsService.getSalesByRange(100L, start, end);

        assertNotNull(range);
        List<?> sales = (List<?>) range.get("sales");
        assertEquals(2, sales.size());

        Map<?, ?> summary = (Map<?, ?>) range.get("summary");
        assertEquals(new BigDecimal("1100000"), summary.get("totalRevenue"));
        assertEquals(5, summary.get("totalOrders"));
        assertEquals(3L, summary.get("days")); // Sept 1 to Sept 3 is 3 days
    }
}
