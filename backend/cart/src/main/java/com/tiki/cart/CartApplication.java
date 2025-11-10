package com.tiki.cart;

import com.tiki.common.config.CommonSecurityAutoConfiguration;
import com.tiki.common.filter.JwtAuthenticationFilter;
import com.tiki.common.security.JwtTokenProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {CommonSecurityAutoConfiguration.class})
@EnableFeignClients
public class CartApplication {
    
    // JWT beans - Direct definition for simplicity
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
