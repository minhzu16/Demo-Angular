package com.tiki.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Order Tracking Controller
 * Provides order tracking and delivery status information
 */
@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
public class OrderTrackingController {
    
    /**
     * Get order tracking information
     * GET /api/v1/orders/{orderId}/tracking
     */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getTrackingInfo(@PathVariable Integer orderId) {
        log.info("Getting tracking info for order: {}", orderId);
        
        // Return mock tracking data
        Map<String, Object> tracking = Map.of(
            "orderId", orderId,
            "trackingNumber", "TK" + orderId + "123456",
            "carrier", "Giao Hàng Nhanh",
            "status", "IN_TRANSIT",
            "currentLocation", "TP Hồ Chí Minh",
            "estimatedDelivery", LocalDateTime.now().plusDays(2).toString(),
            "trackingHistory", List.of(
                Map.of(
                    "status", "ORDER_PLACED",
                    "location", "Shop",
                    "timestamp", LocalDateTime.now().minusDays(1).toString()
                ),
                Map.of(
                    "status", "PICKED_UP",
                    "location", "Warehouse",
                    "timestamp", LocalDateTime.now().minusHours(12).toString()
                ),
                Map.of(
                    "status", "IN_TRANSIT",
                    "location", "TP Hồ Chí Minh",
                    "timestamp", LocalDateTime.now().minusHours(2).toString()
                )
            )
        );
        
        return ResponseEntity.ok(tracking);
    }
    
    /**
     * Get delivery status
     * GET /api/v1/orders/{orderId}/delivery-status
     */
    @GetMapping("/{orderId}/delivery-status")
    public ResponseEntity<?> getDeliveryStatus(@PathVariable Integer orderId) {
        log.info("Getting delivery status for order: {}", orderId);
        
        // Return mock delivery status
        Map<String, Object> status = Map.of(
            "orderId", orderId,
            "status", "IN_TRANSIT",
            "deliveryProgress", 60,
            "estimatedDelivery", LocalDateTime.now().plusDays(2).toString(),
            "canCancel", true,
            "canReturn", false,
            "message", "Your order is on the way"
        );
        
        return ResponseEntity.ok(status);
    }
}
