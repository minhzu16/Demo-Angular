package com.tiki.order;

import com.tiki.common.config.CommonSecurityAutoConfiguration;
import com.tiki.common.filter.JwtAuthenticationFilter;
import com.tiki.common.security.JwtTokenProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {CommonSecurityAutoConfiguration.class})
@EnableFeignClients
public class OrderApplication {
	
	// Define JWT beans directly - Simple & Fast approach
	@Bean
	public JwtTokenProvider jwtTokenProvider() {
		return new JwtTokenProvider();
	}
	
	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		return new JwtAuthenticationFilter(jwtTokenProvider);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}
