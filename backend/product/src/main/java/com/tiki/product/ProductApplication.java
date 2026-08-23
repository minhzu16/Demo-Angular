package com.tiki.product;

import com.tiki.common.filter.JwtAuthenticationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@EntityScan(basePackages = { "com.tiki.product.entity", "com.tiki.common.entity" })
@EnableJpaRepositories(basePackages = "com.tiki.product.repository")
public class ProductApplication {

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter();
	}

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}
}
