package com.tiki.order.service;

import com.tiki.order.client.PaymentClient;
import com.tiki.order.client.UserClient;
import com.tiki.order.dto.RmaCreateRequest;
import com.tiki.order.dto.RmaItemDto;
import com.tiki.order.dto.RmaResponseDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.RmaEntity;
import com.tiki.order.entity.RmaItemEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.RmaItemRepository;
import com.tiki.order.repository.RmaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RmaServiceTest {

    @Mock
    private RmaRepository rmaRepository;

    @Mock
    private RmaItemRepository rmaItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private RmaService rmaService;

    private OrderEntity deliveredOrder;
    private RmaCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        deliveredOrder = new OrderEntity();
        deliveredOrder.setId(101);
        deliveredOrder.setUserId(10L);
        deliveredOrder.setShopId(5L);
        deliveredOrder.setStatus(OrderEntity.OrderStatus.DELIVERED);
        deliveredOrder.setTotalAmount(new BigDecimal("500000.00"));

        createRequest = RmaCreateRequest.builder()
                .orderId(101)
                .type(RmaEntity.RmaType.REFUND)
                .reasonCategory(RmaEntity.ReasonCategory.DEFECTIVE)
                .reason("Màn hình bị sọc")
                .items(List.of(
                        RmaItemDto.builder()
                                .productId(1L)
                                .productName("Màn hình LCD")
                                .quantity(1)
                                .returnQuantity(1)
                                .unitPrice(new BigDecimal("500000.00"))
                                .build()
                ))
                .build();
    }

    @Test
    void testCreateRma_Success() {
        when(rmaRepository.countByUserIdAndStatusIn(eq(10L), any())).thenReturn(0L);
        when(orderRepository.findById(101)).thenReturn(Optional.of(deliveredOrder));
        when(rmaRepository.save(any(RmaEntity.class))).thenAnswer(inv -> {
            RmaEntity r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(rmaItemRepository.saveAll(any())).thenReturn(Collections.emptyList());

        RmaResponseDto result = rmaService.createRma(10L, createRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(101, result.getOrderId());
        assertEquals(10L, result.getUserId());
        assertEquals(RmaEntity.RmaStatus.RMA_REQUESTED, result.getStatus());
        assertTrue(result.getRmaNumber().startsWith("RMA-"));
    }

    @Test
    void testFullRmaLifecycle_To_Refund() {
        RmaEntity rma = RmaEntity.builder()
                .id(1L)
                .rmaNumber("RMA-20270101-ABC12345")
                .orderId(101)
                .userId(10L)
                .shopId(5L)
                .status(RmaEntity.RmaStatus.RMA_REQUESTED)
                .refundAmount(new BigDecimal("500000.00"))
                .build();

        when(rmaRepository.findById(1L)).thenReturn(Optional.of(rma));
        when(rmaRepository.save(any(RmaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rmaItemRepository.findByRmaId(1L)).thenReturn(Collections.emptyList());

        // 1. Seller approves
        RmaResponseDto approved = rmaService.sellerApproveRma(1L, 5L, "Đồng ý nhận hàng trả lại");
        assertEquals(RmaEntity.RmaStatus.RMA_APPROVED, approved.getStatus());

        // 2. Buyer ships back
        RmaResponseDto shipped = rmaService.updateReturnTracking(1L, 10L, "VNPOST123456", "VNPost");
        assertEquals(RmaEntity.RmaStatus.ITEM_SHIPPED_BACK, shipped.getStatus());
        assertEquals("VNPOST123456", shipped.getReturnTrackingNumber());

        // 3. Warehouse receives
        RmaResponseDto received = rmaService.confirmItemReceived(1L, 999L, "Kiện hàng nguyên vẹn");
        assertEquals(RmaEntity.RmaStatus.ITEM_RECEIVED, received.getStatus());

        // 4. Inspection passed
        RmaResponseDto inspected = rmaService.submitInspection(1L, 999L, RmaEntity.InspectionResult.PASSED, "Lỗi phần cứng xác thực");
        assertEquals(RmaEntity.RmaStatus.INSPECTION_PASSED, inspected.getStatus());

        // 5. Process refund
        RmaResponseDto completed = rmaService.processRefund(1L);
        assertEquals(RmaEntity.RmaStatus.COMPLETED, completed.getStatus());
        verify(paymentClient).refundPayment(101);
        verify(userClient).updatePoints(10L, -500); // 500k / 1000 = 500 points revoked
    }

    @Test
    void testProcessExchange_Success() {
        RmaEntity rma = RmaEntity.builder()
                .id(2L)
                .rmaNumber("RMA-20270101-EX123456")
                .orderId(101)
                .userId(10L)
                .shopId(5L)
                .type(RmaEntity.RmaType.EXCHANGE)
                .status(RmaEntity.RmaStatus.INSPECTION_PASSED)
                .build();

        when(rmaRepository.findById(2L)).thenReturn(Optional.of(rma));
        when(rmaRepository.save(any(RmaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rmaItemRepository.findByRmaId(2L)).thenReturn(Collections.emptyList());

        RmaResponseDto result = rmaService.processExchange(2L, 202);

        assertEquals(RmaEntity.RmaStatus.COMPLETED, result.getStatus());
        assertEquals(202, result.getExchangeOrderId());
    }
}
