package com.tiki.order.controller;

import com.tiki.order.dto.RmaActionDto;
import com.tiki.order.dto.RmaResponseDto;
import com.tiki.order.service.RmaService;
import com.tiki.order.service.ShopOwnershipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RmaControllerAuthTest {

    @Mock private RmaService rmaService;
    @Mock private ShopOwnershipService shopOwnershipService;
    @InjectMocks private RmaController controller;

    private static HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    private RmaResponseDto rma(long userId, long shopId) {
        RmaResponseDto dto = new RmaResponseDto();
        dto.setUserId(userId);
        dto.setShopId(shopId);
        return dto;
    }

    @Test
    @DisplayName("Hoàn tiền / đổi hàng / nhận hàng / kiểm định / danh sách toàn bộ chỉ ADMIN")
    void warehouseAndMoneySteps_areAdminOnly() {
        for (String role : new String[]{"BUYER", "SELLER", null}) {
            assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class, () -> controller.processRefund(1L, role))));
            assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class, () -> controller.processExchange(1L, role, 5))));
            assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class, () -> controller.confirmItemReceived(1L, 9L, role, null))));
            assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class, () -> controller.submitInspection(1L, 9L, role, new RmaActionDto()))));
            assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class, () -> controller.getAllRmas(role, null))));
        }
        verify(rmaService, never()).processRefund(anyLong());

        controller.processRefund(1L, "ROLE_ADMIN");
        verify(rmaService).processRefund(1L);
    }

    @Test
    @DisplayName("Seller chỉ duyệt/từ chối RMA của shop mình")
    void seller_canOnlyDecideOwnShopRmas() {
        when(rmaService.getRmaById(3L)).thenReturn(rma(10, 5));
        when(shopOwnershipService.ownsShop(50L, 5L)).thenReturn(true);

        controller.approveRma(3L, 50L, "SELLER", null);
        verify(rmaService).sellerApproveRma(3L, 50L, "Chấp nhận đổi/trả");

        assertThrows(ResponseStatusException.class, () -> controller.approveRma(3L, 51L, "SELLER", null));
        assertThrows(ResponseStatusException.class, () -> controller.rejectRma(3L, 51L, "SELLER", null));
        verify(rmaService, never()).sellerRejectRma(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("Đọc RMA: chỉ người tạo, chủ shop hoặc admin")
    void readRma_requiresParticipant() {
        when(rmaService.getRmaById(3L)).thenReturn(rma(10, 5));
        when(shopOwnershipService.ownsShop(50L, 5L)).thenReturn(true);
        lenient().when(shopOwnershipService.ownsShop(77L, 5L)).thenReturn(false);

        controller.getRmaById(3L, 10L, "BUYER");       // the buyer who opened it
        controller.getRmaById(3L, 50L, "SELLER");      // owner of the shop
        controller.getRmaById(3L, 1L, "ADMIN");
        assertThrows(ResponseStatusException.class, () -> controller.getRmaById(3L, 77L, "BUYER"));
    }
}
