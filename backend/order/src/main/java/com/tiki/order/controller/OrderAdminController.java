package com.tiki.order.controller;

import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/orders", "/api/v1/admin/orders"})
@Slf4j
public class OrderAdminController {

    @Autowired
    private OrderService orderService;

    @PutMapping("/{orderId}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Integer orderId,
            @PathVariable OrderEntity.OrderStatus status,
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestHeader(value = "X-Username", required = false) String adminUsername) {
        
        // Log admin actions for audit trail
        log.warn("ADMIN ACTION: User {} ({}) updating order {} status to {}", 
            adminUsername != null ? adminUsername : "unknown", adminId, orderId, status);
        
        OrderDto dto = orderService.updateStatus(orderId, status);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Get all orders (admin)
     * GET /api/v1/admin/orders
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin getting all orders");
        
        // Return mock empty page
        return ResponseEntity.ok(Page.empty(PageRequest.of(page, size)));
    }
    
    /**
     * Get pending orders (admin)
     * GET /api/v1/admin/orders/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingOrders() {
        log.info("Admin getting pending orders");
        
        return ResponseEntity.ok(Map.of(
            "orders", List.of(),
            "total", 0,
            "message", "No pending orders"
        ));
    }
    
    /**
     * Get order analytics (admin)
     * GET /api/v1/admin/orders/analytics
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrderAnalytics() {
        log.info("Admin getting order analytics");
        
        Map<String, Object> analytics = Map.of(
            "totalOrders", 0,
            "pendingOrders", 0,
            "processingOrders", 0,
            "completedOrders", 0,
            "cancelledOrders", 0,
            "totalRevenue", 0,
            "averageOrderValue", 0
        );
        
        return ResponseEntity.ok(analytics);
    }
}


