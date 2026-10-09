package com.tiki.analytics.config;

import com.tiki.analytics.client.ShopClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsAccessInterceptorTest {

    @Mock private ShopClient shopClient;
    @InjectMocks private AnalyticsAccessInterceptor interceptor;

    private MockHttpServletRequest request(String userId, String role, String shopId) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/api/v1/analytics/sales/revenue");
        if (userId != null) r.addHeader("X-User-Id", userId);
        if (role != null) r.addHeader("X-User-Role", role);
        if (shopId != null) r.setParameter("shopId", shopId);
        return r;
    }

    private HttpStatus run(MockHttpServletRequest r) {
        try {
            return interceptor.preHandle(r, new MockHttpServletResponse(), new Object()) ? null : HttpStatus.FORBIDDEN;
        } catch (ResponseStatusException e) {
            return HttpStatus.valueOf(e.getStatusCode().value());
        }
    }

    @Test
    @DisplayName("Ẩn danh -> 401")
    void anonymous_isUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, run(request(null, null, "5")));
    }

    @Test
    @DisplayName("Seller xem số liệu shop của mình (user id khác shop id) -> được; shop khác -> 403")
    void seller_canOnlySeeOwnShop() {
        when(shopClient.getShopBySeller(50L)).thenReturn(new ShopClient.ShopRef(5L));

        assertEquals(null, run(request("50", "SELLER", "5")));
        assertEquals(HttpStatus.FORBIDDEN, run(request("50", "SELLER", "6")));
        assertEquals(HttpStatus.FORBIDDEN, run(request("5", "SELLER", "5")));   // user id equal to shop id is not ownership
    }

    @Test
    @DisplayName("Không có shopId = số liệu toàn nền tảng -> chỉ admin")
    void platformWide_isAdminOnly() {
        assertEquals(HttpStatus.FORBIDDEN, run(request("50", "SELLER", null)));
        assertEquals(null, run(request("1", "ADMIN", null)));
        assertEquals(null, run(request("1", "ROLE_ADMIN", "9")));
    }

    @Test
    @DisplayName("shop-service lỗi -> từ chối (fail closed)")
    void lookupFailure_failsClosed() {
        when(shopClient.getShopBySeller(50L)).thenThrow(new RuntimeException("down"));
        assertEquals(HttpStatus.FORBIDDEN, run(request("50", "SELLER", "5")));
        assertTrue(true);
    }
}
