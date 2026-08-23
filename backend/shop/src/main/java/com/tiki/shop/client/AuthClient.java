package com.tiki.shop.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "auth", url = "http://${AUTH_HOST:auth}:${AUTH_PORT:8082}")
public interface AuthClient {

    @PostMapping("/api/v1/users/{id}/role")
    Map<String, String> updateUserRole(
            @PathVariable("id") Long id,
            @RequestParam("role") String role);
}
