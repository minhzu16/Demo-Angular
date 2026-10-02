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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;

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

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // SECURITY FIX (Vulnerability 3.4): Block direct external access to internal service-to-service endpoints
        if (isInternalPath(path)) {
            log.warn("Blocked direct external access attempt to internal path: {}", path);
            return onError(exchange, HttpStatus.FORBIDDEN, "Truy cập tài nguyên nội bộ bị từ chối qua Gateway");
        }

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
                if (isAdminPath(path) && !hasAdminRole(role)) {
                    log.warn("Access denied for user {} with role {} on admin path: {}", username, role, path);
                    return onError(exchange, HttpStatus.FORBIDDEN, "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN");
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
                log.warn("Invalid JWT token on path {}: {}", path, e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
            }
        }

        // No Bearer token provided
        // Strictly protect admin paths from unauthenticated access
        if (isAdminPath(path)) {
            log.warn("Unauthenticated access attempt on admin path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để truy cập tài nguyên này");
        }

        // Strip client headers for all non-authenticated requests to prevent spoofing
        ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Username");
                    headers.remove("X-User-Role");
                    headers.set("X-Request-Id", correlationId);
                })
                .build();

        return chain.filter(exchange.mutate().request(cleanRequest).build());
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/api/v1/admin/") || path.contains("/admin/");
    }

    private boolean isInternalPath(String path) {
        return path != null && (path.contains("/internal/") || path.endsWith("/internal"));
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
