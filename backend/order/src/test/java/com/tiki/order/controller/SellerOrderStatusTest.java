package com.tiki.order.controller;

import com.tiki.common.exception.AccessDeniedException;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.exception.BadRequestException;
import com.tiki.order.service.OrderQueryService;
import com.tiki.order.service.OrderStatusService;
import com.tiki.order.service.ShopOwnershipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Shop ids are NOT user ids: seller user 50 owns shop 5 (as in the seed data, shop 1 belongs to user 2). */
@ExtendWith(MockitoExtension.class)
class SellerOrderStatusTest {

    private static final long SELLER_USER = 50L;
    private static final long SHOP = 5L;

    @Mock private OrderQueryService orderQueryService;
    @Mock private OrderStatusService orderStatusService;
    @Mock private ShopOwnershipService shopOwnershipService;
    @Mock private com.tiki.order.service.OrderAnalyticsService orderAnalyticsService;
    @InjectMocks private OrderController controller;

    private OrderDto order(String status, Long shopId) {
        OrderDto dto = new OrderDto();
        dto.setStatus(status);
        dto.setShopId(shopId);
        return dto;
    }

    private void sellerOwnsShop() {
        lenient().when(shopOwnershipService.ownsShop(SELLER_USER, SHOP)).thenReturn(true);
    }

    @Test
    @DisplayName("Seller sở hữu shop (user id khác shop id): PENDING -> CONFIRMED được chấp nhận")
    void ownSeller_canConfirmPendingOrder() {
        sellerOwnsShop();
        when(orderQueryService.getOrder(7)).thenReturn(order("PENDING", SHOP));
        OrderDto updated = order("CONFIRMED", SHOP);
        when(orderStatusService.updateStatus(7, OrderEntity.OrderStatus.CONFIRMED)).thenReturn(updated);

        assertEquals(updated, controller.updateOrderStatusBySeller(7, Map.of("status", "confirmed"), SELLER_USER, "SELLER"));
    }

    @Test
    @DisplayName("Seller khác (không sở hữu shop) -> AccessDenied, không đổi trạng thái")
    void otherSeller_isDenied() {
        sellerOwnsShop();
        when(orderQueryService.getOrder(7)).thenReturn(order("PENDING", SHOP));

        assertThrows(AccessDeniedException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "CONFIRMED"), 99L, "SELLER"));
        verify(orderStatusService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("User id trùng shop id nhưng không sở hữu shop -> vẫn bị từ chối")
    void userIdEqualToShopId_isNotOwnership() {
        when(orderQueryService.getOrder(7)).thenReturn(order("PENDING", SHOP));

        assertThrows(AccessDeniedException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "CONFIRMED"), SHOP, "SELLER"));
    }

    @Test
    @DisplayName("Đơn chưa gắn shop: seller bị từ chối, admin vẫn được")
    void orderWithoutShop_onlyAdmin() {
        when(orderQueryService.getOrder(7)).thenReturn(order("PENDING", null));
        assertThrows(AccessDeniedException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "CONFIRMED"), SELLER_USER, "SELLER"));

        controller.updateOrderStatusBySeller(7, Map.of("status", "CONFIRMED"), 1L, "ROLE_ADMIN");
        verify(orderStatusService).updateStatus(7, OrderEntity.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Chuyển trạng thái sai chuỗi hoặc ngoài quyền seller -> 400")
    void invalidTransitions_areRejected() {
        sellerOwnsShop();
        when(orderQueryService.getOrder(7)).thenReturn(order("PENDING", SHOP));
        // PENDING -> SHIPPING bỏ qua bước xác nhận
        assertThrows(BadRequestException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "SHIPPING"), SELLER_USER, "SELLER"));
        // DELIVERED / REFUNDED không thuộc quyền seller
        assertThrows(BadRequestException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "DELIVERED"), SELLER_USER, "SELLER"));
        assertThrows(BadRequestException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "REFUNDED"), SELLER_USER, "SELLER"));
        // Giá trị rác
        assertThrows(BadRequestException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "NOPE"), SELLER_USER, "SELLER"));
        verify(orderStatusService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("Không thể hủy đơn đang giao qua endpoint seller")
    void cannotCancelShippingOrder() {
        sellerOwnsShop();
        when(orderQueryService.getOrder(7)).thenReturn(order("SHIPPING", SHOP));
        assertThrows(BadRequestException.class,
                () -> controller.updateOrderStatusBySeller(7, Map.of("status", "CANCELLED"), SELLER_USER, "SELLER"));
    }

    @Test
    @DisplayName("Seller xem thống kê / đơn của shop khác -> bị từ chối; admin được")
    void shopStats_requireOwnership() {
        sellerOwnsShop();
        assertThrows(AccessDeniedException.class, () -> controller.getShopOrderStats(6L, SELLER_USER, "SELLER"));
        assertThrows(AccessDeniedException.class, () -> controller.getShopOrderStatsForSeller(6L, SELLER_USER, "SELLER"));
        controller.getShopOrderStatsForSeller(6L, 1L, "ADMIN"); // admin: no ownership lookup needed
    }
}
