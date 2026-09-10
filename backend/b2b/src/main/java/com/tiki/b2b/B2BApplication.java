package com.tiki.b2b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class B2BApplication {
    public static void main(String[] args) {
        SpringApplication.run(B2BApplication.class, args);
    }
}
