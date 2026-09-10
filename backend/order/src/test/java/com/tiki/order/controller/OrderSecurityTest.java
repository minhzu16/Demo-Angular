package com.tiki.order.controller;

import com.tiki.common.exception.AccessDeniedException;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSecurityTest - Validating Bug 8 IDOR Protection")
public class OrderSecurityTest {

    @Mock
    private OrderCreationService orderCreationService;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private OrderStatusService orderStatusService;

    @Mock
    private OrderAnalyticsService orderAnalyticsService;

    @Mock
    private OrderPaymentService orderPaymentService;

    @Mock
    private MockPaymentService mockPaymentService;

    @InjectMocks
    private OrderController orderController;

    @Test
    @DisplayName("Bug 8 IDOR: getOrder by unauthorized user throws AccessDeniedException")
    void testGetOrder_IDOR_AccessDenied() {
        OrderDto order = new OrderDto();
        order.setId(100);
        order.setUserId(42); // belongs to user 42

        when(orderQueryService.getOrder(100)).thenReturn(order);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 99L); // user 99 is attempting to access
        request.setAttribute("role", "ROLE_BUYER");

        assertThrows(AccessDeniedException.class, () -> {
            orderController.getOrder(100, null, null, request);
        });
    }

    @Test
    @DisplayName("Bug 8 IDOR: cancelOrder by unauthorized user throws AccessDeniedException")
    void testCancelOrder_IDOR_AccessDenied() {
        OrderDto order = new OrderDto();
        order.setId(100);
        order.setUserId(42); // belongs to user 42

        when(orderQueryService.getOrder(100)).thenReturn(order);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 99L); // user 99 is attempting to cancel

        assertThrows(AccessDeniedException.class, () -> {
            orderController.cancelOrder(100, request);
        });
    }

    @Test
    @DisplayName("Bug 8 IDOR: cancelOrder by legitimate owner succeeds")
    void testCancelOrder_LegitimateOwner_Success() {
        OrderDto order = new OrderDto();
        order.setId(100);
        order.setUserId(42);

        when(orderQueryService.getOrder(100)).thenReturn(order);
        when(orderStatusService.cancelOrder(100)).thenReturn(order);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L); // legitimate owner

        OrderDto result = orderController.cancelOrder(100, request);
        assertNotNull(result);
        verify(orderStatusService).cancelOrder(100);
    }
}
