package com.tiki.shop;

import com.tiki.common.filter.JwtAuthenticationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.tiki.shop", "com.tiki.common"})
@EnableFeignClients
@EnableJpaRepositories(basePackages = {"com.tiki.shop", "com.tiki.common.repository"})
@EntityScan(basePackages = {"com.tiki.shop", "com.tiki.common.entity"})
public class ShopApplication {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
	}
}
