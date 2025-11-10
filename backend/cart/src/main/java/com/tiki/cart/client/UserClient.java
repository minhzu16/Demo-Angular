package com.tiki.cart.client;

import com.tiki.common.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="auth-service", url="${services.auth.url:http://localhost:8082}")
public interface UserClient {
    @CircuitBreaker(name = "auth-service", fallbackMethod = "getUserFallback")
    @Retry(name = "auth-service")
    @Bulkhead(name = "auth-service")
    @GetMapping("/api/v1/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);
    
    default UserDto getUserFallback(Long id, Exception e) {
        UserDto fallback = new UserDto();
        fallback.setId(id);
        fallback.setUsername("Unknown User");
        fallback.setEmail("unknown@example.com");
        return fallback;
    }
}
