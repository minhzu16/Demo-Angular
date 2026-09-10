package com.tiki.order.controller;

import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderTrackingEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Order Tracking Controller
 * Provides real-time order tracking and delivery timeline information backed by MySQL.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderTrackingController {

    private final OrderRepository orderRepository;
    private final OrderTrackingRepository orderTrackingRepository;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddTrackingEventRequest {
        private String location;
        private String note;
        private OrderEntity.OrderStatus status;
    }

    /**
     * Get order tracking information
     * GET /api/v1/orders/{orderId}/tracking
     */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getTrackingInfo(@PathVariable Integer orderId) {
        log.info("Getting real tracking timeline for order: {}", orderId);

        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Đơn hàng #" + orderId + " không tồn tại"));
        }

        List<OrderTrackingEntity> entities = orderTrackingRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        List<Map<String, Object>> historyList = new ArrayList<>();

        for (OrderTrackingEntity event : entities) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", event.getStatus() != null ? event.getStatus().name() : "UNKNOWN");
            map.put("location", event.getLocation() != null ? event.getLocation() : "Hệ thống Tiki Smart Logistics");
            map.put("note", event.getNote() != null ? event.getNote() : "");
            map.put("timestamp", event.getCreatedAt() != null ? event.getCreatedAt().toString() : LocalDateTime.now().toString());
            historyList.add(map);
        }

        // If no tracking record found, build initial timeline step
        if (historyList.isEmpty()) {
            Map<String, Object> initial = new LinkedHashMap<>();
            initial.put("status", order.getStatus().name());
            initial.put("location", "Trung tâm xử lý Tiki");
            initial.put("note", "Đơn hàng đã được tạo trên hệ thống");
            initial.put("timestamp", order.getCreatedAt() != null ? order.getCreatedAt().toString() : LocalDateTime.now().toString());
            historyList.add(initial);
        }

        String currentLocation = entities.stream()
                .filter(e -> e.getLocation() != null && !e.getLocation().isBlank())
                .map(OrderTrackingEntity::getLocation)
                .findFirst()
                .orElse("Trung tâm điều phối Tiki");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", orderId);
        response.put("trackingNumber", "TK" + orderId + (order.getCreatedAt() != null ? order.getCreatedAt().getNano() % 100000 : "12345"));
        response.put("carrier", "TikiNOW Smart Logistics");
        response.put("status", order.getStatus().name());
        response.put("currentLocation", currentLocation);
        response.put("estimatedDelivery", order.getCreatedAt() != null ? order.getCreatedAt().plusDays(2).toString() : null);
        response.put("trackingHistory", historyList);

        return ResponseEntity.ok(response);
    }

    /**
     * Get delivery status and action capabilities
     * GET /api/v1/orders/{orderId}/delivery-status
     */
    @GetMapping("/{orderId}/delivery-status")
    public ResponseEntity<?> getDeliveryStatus(@PathVariable Integer orderId) {
        log.info("Getting delivery status for order: {}", orderId);

        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Đơn hàng #" + orderId + " không tồn tại"));
        }

        OrderEntity.OrderStatus status = order.getStatus();
        int progress;
        String message;
        boolean canCancel = false;
        boolean canReturn = false;

        switch (status) {
            case PENDING -> {
                progress = 15;
                message = "Đơn hàng đang chờ người bán xác nhận";
                canCancel = true;
            }
            case CONFIRMED -> {
                progress = 30;
                message = "Người bán đã xác nhận đơn hàng";
                canCancel = true;
            }
            case PROCESSING -> {
                progress = 50;
                message = "Đơn hàng đang được đóng gói tại kho";
            }
            case SHIPPING -> {
                progress = 80;
                message = "Đơn hàng đang trên đường giao đến bạn";
            }
            case DELIVERED -> {
                progress = 100;
                message = "Đơn hàng đã được giao thành công";
                canReturn = true;
            }
            case RETURN_REQUESTED -> {
                progress = 85;
                message = "Khách hàng đã yêu cầu đổi trả / hoàn tiền";
            }
            case REFUNDED -> {
                progress = 100;
                message = "Đơn hàng đã được hoàn tiền";
            }
            case CANCELLED -> {
                progress = 0;
                message = "Đơn hàng đã bị hủy";
            }
            default -> {
                progress = 10;
                message = "Đang xử lý";
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("status", status.name());
        result.put("deliveryProgress", progress);
        result.put("estimatedDelivery", order.getCreatedAt() != null ? order.getCreatedAt().plusDays(2).toString() : null);
        result.put("canCancel", canCancel);
        result.put("canReturn", canReturn);
        result.put("message", message);

        return ResponseEntity.ok(result);
    }

    /**
     * Add tracking timeline milestone
     * POST /api/v1/orders/{orderId}/tracking
     */
    @PostMapping("/{orderId}/tracking")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<?> addTrackingMilestone(
            @PathVariable Integer orderId,
            @RequestBody AddTrackingEventRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long staffId) {

        log.info("Adding tracking milestone for order {}: status={}, location={}", orderId, request.getStatus(), request.getLocation());

        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Đơn hàng #" + orderId + " không tồn tại"));
        }

        OrderTrackingEntity event = new OrderTrackingEntity();
        event.setOrderId(orderId);
        event.setStatus(request.getStatus() != null ? request.getStatus() : order.getStatus());
        event.setLocation(request.getLocation());
        event.setNote(request.getNote());
        event.setUpdatedBy(staffId);
        event.setCreatedAt(LocalDateTime.now());

        OrderTrackingEntity saved = orderTrackingRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
