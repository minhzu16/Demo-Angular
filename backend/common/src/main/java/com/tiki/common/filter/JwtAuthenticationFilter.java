package com.tiki.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

/**
 * Lightweight JWT filter used by cart/order/product services.
 *
 * Responsibility:
 * - Đọc các header đã được Gateway relay (X-User-Id, X-Username).
 * - Đặt các giá trị này vào request attribute ("userId", "username").
 * - Tạo một Authentication với đủ quyền cơ bản để các @PreAuthorize hoạt động.
 *
 * Lưu ý: Việc verify chữ ký JWT đã được xử lý ở Gateway, nên filter này
 * không parse/verify token nữa, chỉ dựa trên header đã được trust trong
 * môi trường nội bộ.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Constructor no-arg cho Spring injection */
    public JwtAuthenticationFilter() {
    }

    /**
     * Constructor backward-compatible: nhận JwtTokenProvider nhưng không dùng,
     * vì JWT validation đã được Gateway xử lý trước.
     */
    @SuppressWarnings("unused")
    public JwtAuthenticationFilter(Object ignoredProvider) {
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy thông tin từ header đã được Gateway relay
        String userIdHeader = request.getHeader("X-User-Id");
        String usernameHeader = request.getHeader("X-Username");
        String roleHeader = request.getHeader("X-User-Role");

        // Đặt attribute để các controller có thể sử dụng (OrderController đang dùng)
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                request.setAttribute("userId", userId);
            } catch (NumberFormatException ignored) {
                // Nếu không parse được thì bỏ qua
            }
        }
        if (usernameHeader != null && !usernameHeader.isBlank()) {
            request.setAttribute("username", usernameHeader);
        }
        if (roleHeader != null && !roleHeader.isBlank()) {
            request.setAttribute("role", roleHeader);
        }

        String authHeader = request.getHeader("Authorization");
        if (userIdHeader == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String secret = System.getenv("JWT_SECRET");
                if (secret == null || secret.isBlank()) {
                    secret = System.getProperty("jwt.secret", "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong12345678");
                }
                Key key = Keys.hmacShaKeyFor(secret.getBytes());
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                userIdHeader = String.valueOf(claims.get("userId"));
                usernameHeader = claims.getSubject();
                if (roleHeader == null) {
                    Object r = claims.get("role");
                    if (r != null) {
                        roleHeader = String.valueOf(r);
                    }
                }
                if (userIdHeader != null && !userIdHeader.isBlank() && !"null".equalsIgnoreCase(userIdHeader)) {
                    request.setAttribute("userId", Long.parseLong(userIdHeader));
                }
                if (usernameHeader != null) {
                    request.setAttribute("username", usernameHeader);
                }
                if (roleHeader != null) {
                    request.setAttribute("role", roleHeader);
                }
            } catch (Exception e) {
                // Ignore invalid tokens
            }
        }

        // Tạo Authentication nếu chưa có:
        // C-03 FIX: Trích xuất đúng vai trò (Role) từ header relay hoặc claims
        // Tuyệt đối không gán cứng cả 3 quyền BUYER, SELLER, ADMIN cho mọi người dùng
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String principal = null;

            if (userIdHeader != null && !userIdHeader.isBlank() && !"null".equalsIgnoreCase(userIdHeader)) {
                principal = (usernameHeader != null && !usernameHeader.isBlank())
                        ? usernameHeader
                        : "user_" + userIdHeader;
            }

            if (principal != null) {
                List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                if (roleHeader != null && !roleHeader.isBlank()) {
                    for (String r : roleHeader.split(",")) {
                        String clean = r.trim().toUpperCase();
                        if (!clean.isEmpty()) {
                            if (!clean.startsWith("ROLE_")) {
                                clean = "ROLE_" + clean;
                            }
                            authorities.add(new SimpleGrantedAuthority(clean));
                        }
                    }
                }
                if (authorities.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_BUYER"));
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(principal, null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        jakarta.servlet.http.HttpServletRequestWrapper wrappedRequest = new jakarta.servlet.http.HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equalsIgnoreCase(name) && request.getAttribute("userId") != null) {
                    return String.valueOf(request.getAttribute("userId"));
                }
                if ("X-Username".equalsIgnoreCase(name) && request.getAttribute("username") != null) {
                    return String.valueOf(request.getAttribute("username"));
                }
                if ("X-User-Role".equalsIgnoreCase(name) && request.getAttribute("role") != null) {
                    return String.valueOf(request.getAttribute("role"));
                }
                return super.getHeader(name);
            }

            @Override
            public java.util.Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equalsIgnoreCase(name) && request.getAttribute("userId") != null) {
                    return java.util.Collections.enumeration(java.util.Collections.singletonList(String.valueOf(request.getAttribute("userId"))));
                }
                if ("X-Username".equalsIgnoreCase(name) && request.getAttribute("username") != null) {
                    return java.util.Collections.enumeration(java.util.Collections.singletonList(String.valueOf(request.getAttribute("username"))));
                }
                if ("X-User-Role".equalsIgnoreCase(name) && request.getAttribute("role") != null) {
                    return java.util.Collections.enumeration(java.util.Collections.singletonList(String.valueOf(request.getAttribute("role"))));
                }
                return super.getHeaders(name);
            }

            @Override
            public java.util.Enumeration<String> getHeaderNames() {
                java.util.List<String> names = java.util.Collections.list(super.getHeaderNames());
                if (!names.contains("X-User-Id") && request.getAttribute("userId") != null) names.add("X-User-Id");
                if (!names.contains("X-Username") && request.getAttribute("username") != null) names.add("X-Username");
                if (!names.contains("X-User-Role") && request.getAttribute("role") != null) names.add("X-User-Role");
                return java.util.Collections.enumeration(names);
            }
        };

        filterChain.doFilter(wrappedRequest, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Skip filter for actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}
