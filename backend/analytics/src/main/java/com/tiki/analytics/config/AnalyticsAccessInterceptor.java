package com.tiki.analytics.config;

import com.tiki.analytics.client.ShopClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Business analytics (sales, revenue, customers, CLV, product performance) are private to the shop:
 *  - a request with ?shopId=N is allowed for the seller who owns shop N (resolved via shop-service — shop ids
 *    are not user ids) and for admins;
 *  - a request without shopId is platform-wide data: admins only.
 * Identity comes from the gateway-validated X-User-Id / X-User-Role headers. Previously every figure of every
 * shop was readable by anyone, including anonymous callers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAccessInterceptor implements HandlerInterceptor {

    private final ShopClient shopClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = request.getHeader("X-User-Role");
        if (role != null && role.toUpperCase().contains("ADMIN")) {
            return true;
        }
        Long callerId = parseLong(request.getHeader("X-User-Id"));
        if (callerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        Long shopId = parseLong(request.getParameter("shopId"));
        if (shopId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Số liệu toàn nền tảng chỉ dành cho quản trị viên");
        }
        boolean owns = false;
        try {
            ShopClient.ShopRef shop = shopClient.getShopBySeller(callerId);
            owns = shop != null && shopId.equals(shop.id());
        } catch (Exception e) {
            log.warn("Could not verify shop ownership for seller {}: {}", callerId, e.getMessage()); // fail closed
        }
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xem số liệu của cửa hàng mình");
        }
        return true;
    }

    private static Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
