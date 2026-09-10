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
