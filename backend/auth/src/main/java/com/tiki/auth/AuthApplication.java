package com.tiki.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.tiki.auth.config.JwtProperties;

// Only scan auth package for components (controllers, services, etc.)
// Common package is only used for entities (no component scanning)
@SpringBootApplication(scanBasePackages = "com.tiki.auth")
@EnableConfigurationProperties(JwtProperties.class)
@EnableFeignClients(basePackages = "com.tiki.auth.client")
// Only enable auth repositories (no common repositories to avoid conflicts)
@EnableJpaRepositories(basePackages = "com.tiki.auth.repository")
@EntityScan(basePackages = {"com.tiki.auth.entity", "com.tiki.common.entity"})
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
