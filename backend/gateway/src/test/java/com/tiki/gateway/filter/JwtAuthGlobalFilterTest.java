package com.tiki.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    private JwtAuthGlobalFilter filter;
    private final String secret = "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong12345678";
    private Key key;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthGlobalFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", secret);
        filter.init();
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String createTestToken(Long userId, String username, String role, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("C-01 / C-02: Client tự gửi header X-User-Id và X-User-Role thì Gateway PHẢI xóa bỏ hoàn toàn")
    void filter_ClientSuppliedHeaders_AreStripped() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products/1")
                .header("X-User-Id", "999")
                .header("X-Username", "hacker")
                .header("X-User-Role", "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ServerWebExchange capturedExchange = captor.getValue();
        HttpHeaders capturedHeaders = capturedExchange.getRequest().getHeaders();

        assertNull(capturedHeaders.getFirst("X-User-Id"), "X-User-Id giả mạo phải bị xóa sạch");
        assertNull(capturedHeaders.getFirst("X-Username"), "X-Username giả mạo phải bị xóa sạch");
        assertNull(capturedHeaders.getFirst("X-User-Role"), "X-User-Role giả mạo phải bị xóa sạch");
    }

    @Test
    @DisplayName("C-02: Truy cập admin endpoint mà không có token -> Bị chặn 401 Unauthorized")
    void filter_Unauthenticated_OnAdminPath_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("C-03: User có vai trò BUYER truy cập admin endpoint -> Bị chặn 403 Forbidden")
    void filter_BuyerRole_OnAdminPath_Returns403() {
        String token = createTestToken(10L, "buyer_user", "BUYER", 3600000);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("C-01 + C-02 + C-03: Admin hợp lệ truy cập admin endpoint -> Được chuyển tiếp và gán header chuẩn")
    void filter_AdminRole_OnAdminPath_AllowsAndSetsHeaders() {
        String token = createTestToken(1L, "admin_user", "ADMIN", 3600000);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "999999") // Giả mạo id khác
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ServerWebExchange captured = captor.getValue();
        HttpHeaders headers = captured.getRequest().getHeaders();

        // Kiểm tra header được gán đúng từ JWT thật (không bị override bởi giá trị giả mạo 999999)
        assertEquals("1", headers.getFirst("X-User-Id"));
        assertEquals("admin_user", headers.getFirst("X-Username"));
        assertEquals("ADMIN", headers.getFirst("X-User-Role"));
    }

    private org.springframework.http.HttpStatusCode run(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
        filter.filter(exchange, chain).block();
        return exchange.getResponse().getStatusCode();
    }

    @Test
    @DisplayName("B1: Endpoint nội bộ đổi role/điểm không được gọi từ bên ngoài (kể cả ẩn danh) -> 403")
    void internalOnlyEndpoints_AreBlocked() {
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/users/1/role?role=ADMIN").build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/users/1/points?points=999999").build()));
        String adminToken = createTestToken(1L, "admin", "ADMIN", 3600000);
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/users/1/role?role=ADMIN")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken).build()));
    }

    @Test
    @DisplayName("B6: Matrix param / dấu // không né được chặn endpoint nội bộ và admin")
    void pathTricks_DoNotBypassPolicy() {
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/users/1;x=1/role").build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1//users/1/role").build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.get("/api/v1/products/internal;a=b/pricing-batch").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.get("/api/v1/admin;x=1/orders").build()));
    }

    @Test
    @DisplayName("B2: Thao tác tiền chỉ ADMIN; thanh toán/ví yêu cầu đăng nhập; webhook SePay vẫn mở")
    void paymentEndpoints_RequireAuthOrAdmin() {
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.post("/api/v1/store-credit/add").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.get("/api/v1/store-credit/balance").build()));
        String buyer = createTestToken(10L, "buyer", "BUYER", 3600000);
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/store-credit/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer).build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/payments/confirm/pi_1/COMPLETED")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer).build()));
        assertNull(run(MockServerHttpRequest.get("/api/v1/store-credit/balance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer).build()));
        assertNull(run(MockServerHttpRequest.post("/api/v1/payments/sepay-webhook").build()));
        String admin = createTestToken(1L, "admin", "ADMIN", 3600000);
        assertNull(run(MockServerHttpRequest.post("/api/v1/store-credit/add")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin).build()));
    }

    @Test
    @DisplayName("Audit: warehouse ghi kho = SELLER/ADMIN, b2b verify/giá = ADMIN, live viewers chặn, notifications cần đăng nhập")
    void auditRules_WarehouseB2bLiveNotifications() {
        String buyer = createTestToken(10L, "buyer", "BUYER", 3600000);
        String seller = createTestToken(20L, "seller", "SELLER", 3600000);
        String admin = createTestToken(1L, "admin", "ADMIN", 3600000);
        java.util.function.Function<String, String> bearer = t -> "Bearer " + t;

        // Warehouse stock writes
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.put("/api/v1/warehouse/products/5").build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.put("/api/v1/warehouse/products/5")
                .header(HttpHeaders.AUTHORIZATION, bearer.apply(buyer)).build()));
        assertNull(run(MockServerHttpRequest.put("/api/v1/warehouse/products/5")
                .header(HttpHeaders.AUTHORIZATION, bearer.apply(seller)).build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/warehouse/locations")
                .header(HttpHeaders.AUTHORIZATION, bearer.apply(seller)).build()));
        // Public stock read stays open
        assertNull(run(MockServerHttpRequest.get("/api/v1/warehouse/products/5").build()));

        // B2B
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.post("/api/v1/b2b/companies/1/verify").build()));
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/b2b/companies/1/verify")
                .header(HttpHeaders.AUTHORIZATION, bearer.apply(buyer)).build()));
        assertNull(run(MockServerHttpRequest.post("/api/v1/b2b/companies/1/verify")
                .header(HttpHeaders.AUTHORIZATION, bearer.apply(admin)).build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.get("/api/v1/b2b/purchase-orders/1").build()));

        // Live + notifications
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.post("/api/v1/live/sessions/1/viewers?count=99999").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.post("/api/v1/live/sessions/1/start").build()));
        assertNull(run(MockServerHttpRequest.get("/api/v1/live/sessions/active").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.get("/api/v1/notifications/user/7").build()));
        assertNull(run(MockServerHttpRequest.get("/api/v1/notifications/user/7")
                .header(HttpHeaders.AUTHORIZATION, bearer.apply(buyer)).build()));
    }

    @Test
    @DisplayName("Shop: duyệt hồ sơ người bán chỉ ADMIN; tạo/sửa shop cần đăng nhập")
    void sellerApplicationReview_isAdminOnly() {
        String buyer = createTestToken(10L, "buyer", "BUYER", 3600000);
        String admin = createTestToken(1L, "admin", "ADMIN", 3600000);
        for (MockServerHttpRequest.BaseBuilder<?> b : java.util.List.of(
                MockServerHttpRequest.put("/api/v1/seller-applications/5/approve"),
                MockServerHttpRequest.put("/api/v1/seller-applications/5/reject"),
                MockServerHttpRequest.post("/api/v1/seller-applications/5/review"),
                MockServerHttpRequest.get("/api/v1/seller-applications?status=PENDING"))) {
            assertEquals(HttpStatus.UNAUTHORIZED, run(b.build()));
        }
        assertEquals(HttpStatus.FORBIDDEN, run(MockServerHttpRequest.put("/api/v1/seller-applications/5/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer).build()));
        assertNull(run(MockServerHttpRequest.put("/api/v1/seller-applications/5/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin).build()));
        // applying (own application) only needs a login
        assertNull(run(MockServerHttpRequest.post("/api/v1/seller-applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyer).build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.put("/api/v1/shops/1").build()));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.post("/api/v1/shops").build()));
        // public shop page stays public
        assertNull(run(MockServerHttpRequest.get("/api/v1/shops/1").build()));
    }

    @Test
    @DisplayName("B5: Token hết hạn trên GET catalog công khai -> vẫn phục vụ như khách; trên API riêng tư vẫn 401")
    void expiredToken_OnPublicRead_IsServedAnonymously() {
        String expired = createTestToken(10L, "buyer", "BUYER", -60000);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertNull(captor.getValue().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals(HttpStatus.UNAUTHORIZED, run(MockServerHttpRequest.get("/api/v1/orders/my-orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired).build()));
    }

    @Test
    @DisplayName("Token JWT không hợp lệ hoặc bị sửa đổi -> Bị chặn 401 Unauthorized")
    void filter_InvalidToken_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.fake.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }
}
