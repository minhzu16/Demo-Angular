package com.tiki.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

/**
 * Global filter for JWT authentication at the API Gateway level.
 *
 * Responsibilities:
 * 1. ALWAYS strip incoming client-supplied identity headers (X-User-Id, X-Username, X-User-Role)
 *    to prevent identity spoofing (C-01, C-02).
 * 2. Validate JWT signature and expiration when Authorization: Bearer token is provided.
 * 3. Extract claims (userId, username, role) from validated token and forward as trusted internal headers.
 * 4. Reject invalid/expired tokens with 401 Unauthorized.
 * 5. Guard admin endpoints against unauthorized/unprivileged access (401/403).
 */
@Component
@Slf4j
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong12345678}")
    private String jwtSecret;

    private Key signingKey;

    private enum Access { BLOCKED, ADMIN, SELLER, AUTHENTICATED }

    /** access == null means "explicitly open" (overrides a broader rule that follows). */
    private record Rule(HttpMethod method, String pattern, Access access) {
        static Rule any(String pattern, Access access) { return new Rule(null, pattern, access); }
        static Rule of(HttpMethod method, String pattern, Access access) { return new Rule(method, pattern, access); }
    }

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * Edge policy for endpoints whose services expose them without their own authentication.
     * BLOCKED = service-to-service only (called through Feign, never by a browser) -> always 403.
     * ADMIN = money-moving / privileged operations. AUTHENTICATED = a valid JWT is required.
     * First match wins, so specific rules precede the broad prefixes.
     */
    private static final List<Rule> POLICY = List.of(
            Rule.of(HttpMethod.POST, "/api/v1/users/*/role", Access.BLOCKED),
            Rule.of(HttpMethod.POST, "/api/v1/users/*/points", Access.BLOCKED),
            Rule.any("/api/v1/membership/check/*", Access.BLOCKED),
            Rule.any("/api/v1/membership/use-freeship/*", Access.BLOCKED),
            Rule.of(HttpMethod.POST, "/api/v1/gift-cards/apply", Access.BLOCKED),
            Rule.of(HttpMethod.POST, "/api/v1/store-credit/deduct", Access.BLOCKED),
            Rule.any("/api/v1/warehouse/reserve/**", Access.BLOCKED),
            Rule.any("/api/v1/warehouse/confirm/**", Access.BLOCKED),
            Rule.any("/api/v1/warehouse/release/**", Access.BLOCKED),
            Rule.of(HttpMethod.POST, "/api/v1/settlement/calculate", Access.BLOCKED),
            Rule.of(HttpMethod.POST, "/api/v1/notifications/stock-alert/trigger", Access.BLOCKED),
            // Live: viewer counts are server-side data (no browser should set them); the rest of the
            // write API needs a signed-in user because the service now requires X-User-Id.
            Rule.of(HttpMethod.POST, "/api/v1/live/sessions/*/viewers", Access.BLOCKED),
            Rule.of(HttpMethod.POST, "/api/v1/live/**", Access.AUTHENTICATED),
            Rule.of(HttpMethod.DELETE, "/api/v1/live/**", Access.AUTHENTICATED),
            // Notifications are personal: the service checks ownership, so a token is mandatory.
            Rule.any("/api/v1/notifications/user/**", Access.AUTHENTICATED),
            Rule.of(HttpMethod.PUT, "/api/v1/notifications/*/read", Access.AUTHENTICATED),
            Rule.of(HttpMethod.POST, "/api/v1/payments/confirm/**", Access.ADMIN),
            Rule.of(HttpMethod.PUT, "/api/v1/payments/order/*/status", Access.ADMIN),
            Rule.of(HttpMethod.POST, "/api/v1/payments/order/*/refund", Access.ADMIN),
            Rule.of(HttpMethod.POST, "/api/v1/store-credit/add", Access.ADMIN),
            Rule.of(HttpMethod.POST, "/api/v1/gift-cards/*/reload", Access.ADMIN),
            // Approving a seller application grants the SELLER role -> admin only; applying/editing a shop needs login.
            Rule.of(HttpMethod.GET, "/api/v1/seller-applications", Access.ADMIN),
            Rule.of(HttpMethod.POST, "/api/v1/seller-applications/*/review", Access.ADMIN),
            Rule.of(HttpMethod.PUT, "/api/v1/seller-applications/*/approve", Access.ADMIN),
            Rule.of(HttpMethod.PUT, "/api/v1/seller-applications/*/reject", Access.ADMIN),
            Rule.any("/api/v1/seller-applications/**", Access.AUTHENTICATED),
            Rule.of(HttpMethod.POST, "/api/v1/shops", Access.AUTHENTICATED),
            Rule.of(HttpMethod.PUT, "/api/v1/shops/*", Access.AUTHENTICATED),
            Rule.of(HttpMethod.GET, "/api/v1/shops/my-shop", Access.AUTHENTICATED),
            // B2B: company verification (credit limit) and wholesale price tiers are platform-admin only.
            Rule.of(HttpMethod.POST, "/api/v1/b2b/companies/*/verify", Access.ADMIN),
            Rule.of(HttpMethod.POST, "/api/v1/b2b/prices", Access.ADMIN),
            Rule.any("/api/v1/b2b/**", Access.AUTHENTICATED),
            // Warehouse stock management: the service itself has no authentication.
            Rule.of(HttpMethod.POST, "/api/v1/warehouse/locations", Access.ADMIN),
            Rule.of(HttpMethod.POST, "/api/v1/warehouse/allocate", Access.ADMIN),
            Rule.of(HttpMethod.PUT, "/api/v1/warehouse/stock-by-warehouse/**", Access.ADMIN),
            Rule.of(HttpMethod.PUT, "/api/v1/warehouse/stock/**", Access.SELLER),
            Rule.of(HttpMethod.PUT, "/api/v1/warehouse/products/*", Access.SELLER),
            Rule.any("/api/v1/warehouse/shops/**", Access.SELLER),
            // The payment provider webhook authenticates itself with its own API key.
            Rule.any("/api/v1/payments/sepay-webhook", null),
            Rule.any("/api/v1/payments/**", Access.AUTHENTICATED),
            Rule.any("/api/v1/gift-cards/**", Access.AUTHENTICATED),
            Rule.any("/api/v1/store-credit/**", Access.AUTHENTICATED)
    );

    /** Read-only catalogue calls that must keep working when the browser still holds a stale token. */
    private static final List<String> PUBLIC_READ_PATTERNS = List.of(
            "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**", "/api/v1/flash-sales/**",
            "/api/v1/reviews/product/**", "/api/v1/analytics/recommendations/trending",
            "/api/v1/templates/**", "/api/v1/marketing/**", "/api/products/**"
    );

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // Normalise before matching: matrix params (";x=1") and duplicate slashes must not hide a protected path.
        String path = normalizePath(request.getURI().getPath());
        HttpMethod method = request.getMethod();
        Access required = requiredAccess(method, path);

        // SECURITY FIX (Vulnerability 3.4): Block direct external access to internal service-to-service endpoints
        if (isInternalPath(path) || required == Access.BLOCKED) {
            log.warn("Blocked direct external access attempt to internal path: {}", path);
            return onError(exchange, HttpStatus.FORBIDDEN, "Truy cập tài nguyên nội bộ bị từ chối qua Gateway");
        }
        final boolean adminRequired = isAdminPath(path) || required == Access.ADMIN;
        final boolean sellerRequired = required == Access.SELLER;
        final boolean authRequired = adminRequired || sellerRequired || required == Access.AUTHENTICATED;

        String reqId = request.getHeaders().getFirst("X-Request-Id");
        if (reqId == null || reqId.isBlank()) {
            reqId = java.util.UUID.randomUUID().toString();
        }
        final String correlationId = reqId;
        exchange.getResponse().getHeaders().set("X-Request-Id", correlationId);

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                Object userIdObj = claims.get("userId");
                String userId = userIdObj != null ? String.valueOf(userIdObj) : null;
                String username = claims.getSubject();
                Object roleObj = claims.get("role");
                String role = roleObj != null ? String.valueOf(roleObj) : "BUYER";

                // Check admin authorization if requesting admin endpoint
                if (adminRequired && !hasAdminRole(role)) {
                    log.warn("Access denied for user {} with role {} on admin path: {}", username, role, path);
                    return onError(exchange, HttpStatus.FORBIDDEN, "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN");
                }

                if (sellerRequired && !hasSellerRole(role) && !hasAdminRole(role)) {
                    log.warn("Access denied for user {} with role {} on seller path: {}", username, role, path);
                    return onError(exchange, HttpStatus.FORBIDDEN, "Quyền truy cập bị từ chối: Yêu cầu quyền người bán");
                }

                // Mutate request: Strip client headers and set trusted headers from validated token
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.remove("X-Username");
                            headers.remove("X-User-Role");
                            headers.set("X-Request-Id", correlationId);
                            if (userId != null && !userId.isBlank()) {
                                headers.set("X-User-Id", userId);
                            }
                            if (username != null && !username.isBlank()) {
                                headers.set("X-Username", username);
                            }
                            if (role != null && !role.isBlank()) {
                                headers.set("X-User-Role", role);
                            }
                        })
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (JwtException | IllegalArgumentException e) {
                // A stale token must not break anonymous catalogue browsing: serve it as a guest.
                if (!authRequired && isPublicRead(method, path)) {
                    log.debug("Ignoring invalid token on public read {}: {}", path, e.getMessage());
                    return chain.filter(exchange.mutate().request(stripIdentity(exchange, correlationId)).build());
                }
                log.warn("Invalid JWT token on path {}: {}", path, e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
            }
        }

        // No Bearer token provided
        // Strictly protect admin / money / payment paths from unauthenticated access
        if (authRequired) {
            log.warn("Unauthenticated access attempt on protected path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để truy cập tài nguyên này");
        }

        // Strip client headers for all non-authenticated requests to prevent spoofing
        return chain.filter(exchange.mutate().request(stripIdentity(exchange, correlationId)).build());
    }

    private ServerHttpRequest stripIdentity(ServerWebExchange exchange, String correlationId) {
        return exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Username");
                    headers.remove("X-User-Role");
                    headers.set("X-Request-Id", correlationId);
                })
                .build();
    }

    /** Drops path parameters (";…" per segment) and collapses repeated slashes. */
    static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll(";[^/]*", "").replaceAll("/{2,}", "/");
    }

    private Access requiredAccess(HttpMethod method, String path) {
        for (Rule rule : POLICY) {
            if ((rule.method() == null || rule.method().equals(method)) && MATCHER.match(rule.pattern(), path)) {
                return rule.access();
            }
        }
        return null;
    }

    private boolean isPublicRead(HttpMethod method, String path) {
        return HttpMethod.GET.equals(method) && PUBLIC_READ_PATTERNS.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/api/v1/admin/") || path.contains("/admin/");
    }

    private boolean isInternalPath(String path) {
        return path != null && (path.contains("/internal/") || path.endsWith("/internal"));
    }

    private boolean hasSellerRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return Arrays.stream(role.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("SELLER") || r.equalsIgnoreCase("ROLE_SELLER"));
    }

    private boolean hasAdminRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return Arrays.stream(role.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format(
                "{\"timestamp\":%d,\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                System.currentTimeMillis(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run with highest precedence before other filters
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
