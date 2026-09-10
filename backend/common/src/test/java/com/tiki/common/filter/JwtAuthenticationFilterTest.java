package com.tiki.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("C-03 FIX: Request có X-User-Role là BUYER -> CHỈ nhận quyền ROLE_BUYER, KHÔNG có ROLE_ADMIN hay ROLE_SELLER")
    void doFilter_WhenRoleIsBuyer_OnlyRoleBuyerIsGranted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "10");
        request.addHeader("X-Username", "normal_buyer");
        request.addHeader("X-User-Role", "BUYER");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication phải được tạo");
        assertEquals("normal_buyer", auth.getName());

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertTrue(authorities.contains("ROLE_BUYER"), "Phải có ROLE_BUYER");
        assertFalse(authorities.contains("ROLE_ADMIN"), "C-03 FIX: User thường TUYỆT ĐỐI KHÔNG ĐƯỢC có ROLE_ADMIN");
        assertFalse(authorities.contains("ROLE_SELLER"), "C-03 FIX: User thường TUYỆT ĐỐI KHÔNG ĐƯỢC có ROLE_SELLER");
    }

    @Test
    @DisplayName("C-03 FIX: Request có X-User-Role là ADMIN -> Nhận đúng quyền ROLE_ADMIN")
    void doFilter_WhenRoleIsAdmin_RoleAdminIsGranted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-Username", "system_admin");
        request.addHeader("X-User-Role", "ADMIN");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertTrue(authorities.contains("ROLE_ADMIN"), "Admin phải có ROLE_ADMIN");
    }

    @Test
    @DisplayName("C-03 FIX: Request có nhiều role (BUYER,SELLER) -> Nhận đúng các role đó, không bị thừa ROLE_ADMIN")
    void doFilter_WhenMultipleRoles_CorrectRolesGranted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "5");
        request.addHeader("X-Username", "shop_owner");
        request.addHeader("X-User-Role", "BUYER,SELLER");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertTrue(authorities.contains("ROLE_BUYER"));
        assertTrue(authorities.contains("ROLE_SELLER"));
        assertFalse(authorities.contains("ROLE_ADMIN"), "Người bán không được tự động có quyền ADMIN");
    }

    @Test
    @DisplayName("C-03 FIX: Request không truyền role -> Mặc định chỉ nhận ROLE_BUYER, an toàn tuyệt đối")
    void doFilter_WhenNoRoleHeader_DefaultsToRoleBuyerOnly() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "20");
        request.addHeader("X-Username", "anonymous_user");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertEquals(List.of("ROLE_BUYER"), authorities, "Mặc định chỉ có ROLE_BUYER");
    }
}
