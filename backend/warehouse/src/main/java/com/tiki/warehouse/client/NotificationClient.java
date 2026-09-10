package com.tiki.warehouse.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${NOTIFICATION_SERVICE_URL:http://localhost:8085}")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/stock-alert/trigger")
    Map<String, Object> triggerRestockAlert(@RequestParam("productId") Long productId);
}
