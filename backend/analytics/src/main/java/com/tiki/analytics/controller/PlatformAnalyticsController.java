package com.tiki.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class PlatformAnalyticsController {

    @GetMapping("/platform")
    public ResponseEntity<Map<String, Object>> getPlatformAnalytics() {
        log.info("Admin fetching platform analytics");
        // Mock data for Platform Analytics (GMV, DAU, etc)
        // In a real scenario, this would query from aggregated tables or elasticsearch
        return ResponseEntity.ok(Map.of(
                "gmv", new BigDecimal("1500000000"), // Gross Merchandise Value
                "dau", 5430, // Daily Active Users
                "mau", 125000, // Monthly Active Users
                "totalOrders", 15400,
                "totalSellers", 350,
                "totalProducts", 25000
        ));
    }
}
