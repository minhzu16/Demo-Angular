package com.tiki.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Legacy placeholder for common security auto-configuration.
 *
 * Các service (cart/order/product) hiện đang dùng
 * `@SpringBootApplication(exclude = { CommonSecurityAutoConfiguration.class })`
 * nên class này phải khai báo @AutoConfiguration để Spring Boot 3 cho phép
 * exclude.
 * Không khai báo bean nào ở đây để tránh ảnh hưởng cấu hình security
 * tuỳ biến của từng service.
 */
@AutoConfiguration
@ConditionalOnMissingBean(CommonSecurityAutoConfiguration.class)
public class CommonSecurityAutoConfiguration {
    // Intentionally empty
}
