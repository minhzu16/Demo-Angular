package com.tiki.order.service;

import com.tiki.order.dto.*;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.VoucherEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopVoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private VoucherService voucherService;

    @InjectMocks
    private OrderAnalyticsService orderAnalyticsService;

    private VoucherEntity shopVoucher;

    @BeforeEach
    void setUp() {
        shopVoucher = new VoucherEntity();
        shopVoucher.setId(1);
        shopVoucher.setCode("SHOP100_SALE");
        shopVoucher.setShopId(100L);
        shopVoucher.setType(VoucherEntity.DiscountType.PERCENTAGE);
        shopVoucher.setValue(BigDecimal.valueOf(10));
        shopVoucher.setMinOrderValue(BigDecimal.valueOf(100000));
        shopVoucher.setStartDate(LocalDateTime.now().minusDays(1));
        shopVoucher.setEndDate(LocalDateTime.now().plusDays(10));
        shopVoucher.setMaxUsage(100);
        shopVoucher.setUsedCount(5);
        shopVoucher.setIsActive(true);
    }

    @Test
    @DisplayName("Validate Voucher - Thành công khi mã shop trùng khớp")
    void testValidateVoucher_MatchingShop_Success() {
        when(voucherRepository.findByCodeIgnoreCase("SHOP100_SALE")).thenReturn(Optional.of(shopVoucher));

        ValidateVoucherRequest request = new ValidateVoucherRequest();
        request.setCode("SHOP100_SALE");
        request.setOrderTotal(BigDecimal.valueOf(200000));
        request.setShopId(100L);

        VoucherValidationResponse response = voucherService.validateVoucher(request);

        assertTrue(response.getValid());
        assertNotNull(response.getDiscountAmount());
        assertEquals(0, new BigDecimal("20000").compareTo(response.getDiscountAmount()));
        assertEquals(100L, response.getVoucher().getShopId());
    }

    @Test
    @DisplayName("Validate Voucher - Thất bại khi dùng voucher của Shop A cho đơn hàng của Shop B")
    void testValidateVoucher_DifferentShop_Fails() {
        when(voucherRepository.findByCodeIgnoreCase("SHOP100_SALE")).thenReturn(Optional.of(shopVoucher));

        ValidateVoucherRequest request = new ValidateVoucherRequest();
        request.setCode("SHOP100_SALE");
        request.setOrderTotal(BigDecimal.valueOf(200000));
        request.setShopId(200L); // Khác shop 100L

        VoucherValidationResponse response = voucherService.validateVoucher(request);

        assertFalse(response.getValid());
        assertTrue(response.getMessage().contains("áp dụng cho sản phẩm của cửa hàng mã #100"));
    }

    @Test
    @DisplayName("Create Shop Voucher - Gán đúng shopId khi tạo")
    void testCreateShopVoucher_SetsShopId() {
        CreateVoucherRequest request = new CreateVoucherRequest();
        request.setCode("SHOP50_NEW");
        request.setType(VoucherEntity.DiscountType.FIXED);
        request.setValue(BigDecimal.valueOf(50000));
        request.setMinOrderValue(BigDecimal.valueOf(150000));
        request.setStartDate(LocalDateTime.now());
        request.setEndDate(LocalDateTime.now().plusDays(5));
        request.setMaxUsage(50);
        request.setIsActive(true);

        when(voucherRepository.existsByCode("SHOP50_NEW")).thenReturn(false);
        when(voucherRepository.save(any(VoucherEntity.class))).thenAnswer(inv -> {
            VoucherEntity entity = inv.getArgument(0);
            entity.setId(99);
            return entity;
        });

        VoucherDTO dto = voucherService.createShopVoucher(50L, request);

        assertNotNull(dto);
        assertEquals(50L, dto.getShopId());
        assertEquals("SHOP50_NEW", dto.getCode());
    }

    @Test
    @DisplayName("Get Vouchers By Shop - Trả về danh sách voucher của đúng shop")
    void testGetVouchersByShop() {
        when(voucherRepository.findByShopId(100L)).thenReturn(List.of(shopVoucher));

        List<VoucherDTO> result = voucherService.getVouchersByShop(100L);

        assertEquals(1, result.size());
        assertEquals("SHOP100_SALE", result.get(0).getCode());
        assertEquals(100L, result.get(0).getShopId());
    }

    @Test
    @DisplayName("Order Stats Isolation - Tính toán đúng doanh số riêng biệt theo shop")
    void testShopOrderStats_Isolation() {
        when(orderRepository.countTodayOrdersByShopId(eq(100L), any(LocalDateTime.class))).thenReturn(5);
        when(orderRepository.calculateTodayRevenueByShopId(eq(100L), any(LocalDateTime.class))).thenReturn(new BigDecimal("1500000"));
        when(orderRepository.countPendingOrdersByShopId(100L)).thenReturn(2);
        when(orderRepository.countByShopId(100L)).thenReturn(40L);
        when(orderRepository.calculateTotalRevenueByShopId(100L)).thenReturn(new BigDecimal("25000000"));

        OrderStatsDTO stats = orderAnalyticsService.getShopOrderStats(100L);

        assertNotNull(stats);
        assertEquals(100L, stats.getShopId());
        assertEquals(5, stats.getTodayOrders());
        assertEquals(new BigDecimal("1500000"), stats.getTodayRevenue());
        assertEquals(2, stats.getPendingOrders());
        assertEquals(40, stats.getTotalOrders());
        assertEquals(new BigDecimal("25000000"), stats.getTotalRevenue());
    }

    @Test
    @DisplayName("Product Sold Count - Lấy chính xác từ OrderItemEntity")
    void testProductSoldCountReal() {
        when(orderRepository.getProductSoldCountReal(101L)).thenReturn(85);

        Integer soldCount = orderAnalyticsService.getProductSoldCount(101L);

        assertEquals(85, soldCount);
    }
}
