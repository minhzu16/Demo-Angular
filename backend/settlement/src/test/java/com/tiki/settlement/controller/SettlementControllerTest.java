package com.tiki.settlement.controller;

import com.tiki.settlement.client.ShopClient;
import com.tiki.settlement.dto.PayoutConfirmDto;
import com.tiki.settlement.service.CommissionRuleService;
import com.tiki.settlement.service.PayoutService;
import com.tiki.settlement.service.SettlementCalculationService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementControllerTest {

    @Mock private CommissionRuleService commissionRuleService;
    @Mock private SettlementCalculationService settlementCalculationService;
    @Mock private PayoutService payoutService;
    @Mock private ShopClient shopClient;
    @InjectMocks private SettlementController controller;

    private static HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    @DisplayName("Duyệt/xác nhận chi trả và sửa luật hoa hồng chỉ ADMIN")
    void moneyOperations_areAdminOnly() {
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.approvePayout(1L, "SELLER"))));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.confirmPayout(1L, null, new PayoutConfirmDto()))));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.deleteRule("BUYER", 3L))));
        verify(payoutService, never()).approvePayout(any());
        verify(commissionRuleService, never()).deleteRule(any());

        controller.approvePayout(1L, "ROLE_ADMIN");
        verify(payoutService).approvePayout(1L);
    }

    @Test
    @DisplayName("Seller chỉ yêu cầu chi trả / xem quyết toán của shop mình (id shop khác id user)")
    void shopEndpoints_requireOwnership() {
        when(shopClient.getShopBySeller(50L)).thenReturn(new ShopClient.ShopRef(5L));

        controller.getShopSummary(5L, "2027-01", 50L, "SELLER");           // own shop: ok
        verify(settlementCalculationService).generatePeriodSummary(5L, "2027-01");

        assertThrows(ResponseStatusException.class, () -> controller.getShopSummary(6L, "2027-01", 50L, "SELLER"));
        assertThrows(ResponseStatusException.class, () -> controller.getPayoutHistory(6L, 50L, "SELLER"));
        assertThrows(ResponseStatusException.class,
                () -> controller.initiatePayout(6L, "2027-01", "123", "Bank", "Me", 50L, "SELLER"));
        assertThrows(ResponseStatusException.class, () -> controller.getShopSummary(5L, "2027-01", null, null));
        verify(payoutService, never()).initiatePayout(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Không xác minh được chủ shop (shop-service lỗi) -> từ chối; admin không cần tra cứu")
    void shopLookupFailure_failsClosed_adminBypasses() {
        when(shopClient.getShopBySeller(50L)).thenThrow(new RuntimeException("down"));
        assertThrows(ResponseStatusException.class, () -> controller.getPayoutHistory(5L, 50L, "SELLER"));

        controller.getPayoutHistory(5L, 1L, "ADMIN");
        verify(payoutService).getPayoutHistory(5L);
    }
}
