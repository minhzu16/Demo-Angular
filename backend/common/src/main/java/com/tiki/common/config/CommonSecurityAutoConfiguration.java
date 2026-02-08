package com.tiki.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/**
 * Legacy placeholder for common security auto-configuration.
 *
 * Các service (cart/order/product) hiện đang dùng
 * `@SpringBootApplication(exclude = { CommonSecurityAutoConfiguration.class })`
 * nên class này chỉ cần tồn tại để compile thành công.
 * Không khai báo bean nào ở đây để tránh ảnh hưởng cấu hình security
 * tuỳ biến của từng service.
 */
@Configuration
@ConditionalOnMissingBean(CommonSecurityAutoConfiguration.class)
public class CommonSecurityAutoConfiguration {
    // Intentionally empty
}
