package com.tiki.product.client;

import com.tiki.common.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="common-service", url = "${COMMON_URL:http://common-service}")
public interface UserClient {
    @GetMapping("/api/v1/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);
}
