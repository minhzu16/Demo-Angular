package com.tiki.analytics.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AnalyticsAccessInterceptor analyticsAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Recommendations are handled in RecommendationController (public trending, per-user otherwise).
        registry.addInterceptor(analyticsAccessInterceptor)
                .addPathPatterns("/api/v1/analytics/sales/**", "/api/v1/analytics/customers/**", "/api/v1/analytics/products/**");
    }
}
