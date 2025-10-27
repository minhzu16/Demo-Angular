package com.tiki.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple filter to log request path & parameters while masking sensitive data such as passwords, tokens, phone numbers.
 */
@Component
public class SensitiveLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveLoggingFilter.class);
    private static final List<String> SENSITIVE_KEYS = List.of("password", "token", "authorization", "phone", "email");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Build param string with masking
        String params = Collections.list(request.getParameterNames()).stream()
                .map(name -> name + "=" + maskIfSensitive(name, request.getParameter(name)))
                .collect(Collectors.joining("&"));

        log.info("{} {}{}", request.getMethod(), uri, params.isEmpty() ? "" : ("?" + params));

        filterChain.doFilter(request, response);
    }

    private String maskIfSensitive(String key, String value) {
        if (value == null) return "";
        return SENSITIVE_KEYS.stream().anyMatch(key::equalsIgnoreCase) ? "***" : value;
    }
}
