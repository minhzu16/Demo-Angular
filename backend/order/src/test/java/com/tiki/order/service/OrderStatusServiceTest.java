package com.tiki.order.service;

import com.tiki.order.client.UserClient;
import com.tiki.order.client.WarehouseClient;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderStatusServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTrackingRepository orderTrackingRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private VoucherService voucherService;

    @Mock
    private com.tiki.order.client.PaymentClient paymentClient;

    @InjectMocks
    private OrderStatusService orderStatusService;

    @Test
    void refundOrder_InfiniteLoyaltyPoints_Exploit() {
        OrderEntity order = new OrderEntity();
        order.setId(1);
        order.setUserId(42L);
        order.setStatus(OrderEntity.OrderStatus.DELIVERED); // Đơn hàng ĐÃ GIAO trước đó
        order.setTotalAmount(new BigDecimal("20000000")); // Tivi 20 triệu -> Được cộng 20,000 điểm

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto mockDto = new OrderDto();
        mockDto.setStatus(OrderEntity.OrderStatus.REFUNDED.name());
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(mockDto);

        // Gọi API hoàn tiền (từ DELIVERED -> REFUNDED)
        OrderDto result = orderStatusService.updateStatus(1, OrderEntity.OrderStatus.REFUNDED);

        // Verify:
        // Đã VÁ LỖI: Khi refund, hệ thống PHẢI gọi userClient.updatePoints(42, -20000) để thu hồi điểm.
        verify(userClient, times(1)).updatePoints(eq(42L), eq(-20000));
        // Verify: Gọi payment-service để hoàn tiền
        verify(paymentClient, times(1)).refundPayment(1);
        
        assertEquals(OrderEntity.OrderStatus.REFUNDED.name(), result.getStatus());
    }

    @Test
    void cancelOrder_RefundsSpentPointsAndReleasesVoucher() {
        OrderEntity order = new OrderEntity();
        order.setId(2);
        order.setUserId(99L);
        order.setStatus(OrderEntity.OrderStatus.PENDING);
        order.setUsePoints(500);
        order.setVoucherCode("SALE50K");

        when(orderRepository.findById(2)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto mockDto = new OrderDto();
        mockDto.setStatus(OrderEntity.OrderStatus.CANCELLED.name());
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(mockDto);

        OrderDto result = orderStatusService.cancelOrder(2);

        // Verify that spent points are refunded back to user
        verify(userClient, times(1)).updatePoints(eq(99L), eq(500));
        // Verify that voucher is released
        verify(voucherService, times(1)).releaseVoucher("SALE50K");

        assertEquals(OrderEntity.OrderStatus.CANCELLED.name(), result.getStatus());
    }

    @Test
    void rejectReturn_RevertsToDelivered() {
        OrderEntity order = new OrderEntity();
        order.setId(3);
        order.setStatus(OrderEntity.OrderStatus.RETURN_REQUESTED);

        when(orderRepository.findById(3)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto mockDto = new OrderDto();
        mockDto.setStatus(OrderEntity.OrderStatus.DELIVERED.name());
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(mockDto);

        OrderDto result = orderStatusService.rejectReturn(3, "Hàng đã quá hạn đổi trả 7 ngày");

        assertEquals(OrderEntity.OrderStatus.DELIVERED.name(), result.getStatus());
        verify(orderTrackingRepository).save(argThat(t -> 
                t.getOrderId().equals(3) && 
                t.getStatus() == OrderEntity.OrderStatus.DELIVERED &&
                t.getNote().contains("Hàng đã quá hạn đổi trả 7 ngày")));
    }

    @Test
    void testCancelOrder_WhenAlreadyDelivered_ThrowsIllegalStateException() {
        // Bug 9: Hủy đơn hàng đã giao (DELIVERED) phải bị chặn
        OrderEntity order = new OrderEntity();
        order.setId(4);
        order.setStatus(OrderEntity.OrderStatus.DELIVERED);

        when(orderRepository.findById(4)).thenReturn(Optional.of(order));

        IllegalStateException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> orderStatusService.cancelOrder(4)
        );

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("Không thể hủy đơn hàng #4 vì đơn đã ở trạng thái DELIVERED"));
    }

    @Test
    void testRefundOrder_WhenAlreadyRefunded_ThrowsIllegalStateException() {
        // Bug 10: Hoàn tiền đơn hàng đã hoàn tiền trước đó (Double Refund race condition)
        OrderEntity order = new OrderEntity();
        order.setId(5);
        order.setStatus(OrderEntity.OrderStatus.REFUNDED);

        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        IllegalStateException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> orderStatusService.updateStatus(5, OrderEntity.OrderStatus.REFUNDED)
        );

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("đã hoàn tiền, không thể cập nhật"));
    }
}
