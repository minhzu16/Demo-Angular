package com.tiki.order.service;

import com.tiki.order.dto.RmaCreateRequest;
import com.tiki.order.dto.RmaItemDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.RmaEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.RmaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RmaSecurityTest {

    @Mock
    private RmaRepository rmaRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private RmaService rmaService;

    private RmaCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = RmaCreateRequest.builder()
                .orderId(200)
                .type(RmaEntity.RmaType.REFUND)
                .reasonCategory(RmaEntity.ReasonCategory.DEFECTIVE)
                .reason("Lỗi")
                .items(List.of(
                        RmaItemDto.builder()
                                .productId(1L)
                                .quantity(1)
                                .returnQuantity(1)
                                .unitPrice(BigDecimal.valueOf(100000))
                                .build()
                ))
                .build();
    }

    @Test
    void testAntiAbuse_MaxActiveRmasReached() {
        when(rmaRepository.countByUserIdAndStatusIn(eq(10L), any()))
                .thenReturn(3L); // Already at max 3 active RMAs

        assertThrows(IllegalStateException.class, () ->
                rmaService.createRma(10L, createRequest));
    }

    @Test
    void testIdorProtection_OrderBelongsToOtherUser() {
        when(rmaRepository.countByUserIdAndStatusIn(eq(10L), any())).thenReturn(0L);

        OrderEntity otherUserOrder = new OrderEntity();
        otherUserOrder.setId(200);
        otherUserOrder.setUserId(99L); // Belongs to user 99, but requested by user 10
        otherUserOrder.setStatus(OrderEntity.OrderStatus.DELIVERED);

        when(orderRepository.findById(200)).thenReturn(Optional.of(otherUserOrder));

        assertThrows(SecurityException.class, () ->
                rmaService.createRma(10L, createRequest));
    }

    @Test
    void testValidateOrderStatus_NotDelivered() {
        when(rmaRepository.countByUserIdAndStatusIn(eq(10L), any())).thenReturn(0L);

        OrderEntity pendingOrder = new OrderEntity();
        pendingOrder.setId(200);
        pendingOrder.setUserId(10L);
        pendingOrder.setStatus(OrderEntity.OrderStatus.PROCESSING); // Not delivered yet

        when(orderRepository.findById(200)).thenReturn(Optional.of(pendingOrder));

        assertThrows(IllegalStateException.class, () ->
                rmaService.createRma(10L, createRequest));
    }
}
