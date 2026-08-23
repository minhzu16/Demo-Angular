package com.tiki.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.tiki.warehouse", "com.tiki.common"})
@EnableFeignClients
public class WarehouseApplication {
	public static void main(String[] args) {
		SpringApplication.run(WarehouseApplication.class, args);
	}
}
