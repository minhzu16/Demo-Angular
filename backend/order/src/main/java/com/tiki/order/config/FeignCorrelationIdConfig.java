package com.tiki.order.config;

import com.tiki.common.filter.CorrelationIdFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class FeignCorrelationIdConfig {

    @Bean
    public RequestInterceptor correlationIdRequestInterceptor() {
        return (RequestTemplate template) -> {
            String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            template.header(CorrelationIdFilter.CORRELATION_ID_HEADER, requestId);
            // Internal inter-service call trust header
            template.header("X-User-Role", "INTERNAL");
        };
    }
}
