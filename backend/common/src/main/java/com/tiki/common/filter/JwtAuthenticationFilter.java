package com.tiki.common.filter;

import com.tiki.common.security.JwtTokenProvider;
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
import java.util.Arrays;
import java.util.List;

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

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Lấy thông tin từ header đã được Gateway relay
        String userIdHeader = request.getHeader("X-User-Id");
        String usernameHeader = request.getHeader("X-Username");

        // Đặt attribute để các controller có thể sử dụng (OrderController đang dùng)
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                request.setAttribute("userId", userId);
            } catch (NumberFormatException ignored) {
                // Nếu không parse được thì bỏ qua, giữ nguyên behaviour cũ (null)
            }
        }
        if (usernameHeader != null && !usernameHeader.isBlank()) {
            request.setAttribute("username", usernameHeader);
        }

        // Nếu chưa có Authentication, tạo một principal "system" với đủ quyền
        if (SecurityContextHolder.getContext().getAuthentication() == null && usernameHeader != null) {
            List<GrantedAuthority> authorities = Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_BUYER"),
                    new SimpleGrantedAuthority("ROLE_SELLER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(usernameHeader, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
