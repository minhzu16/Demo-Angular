package com.tiki.order.controller;

import com.tiki.order.dto.CreateOrderRequest;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.dto.OrderStatsDTO;
import com.tiki.order.service.MockPaymentService;
import com.tiki.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.tiki.common.exception.AccessDeniedException;

/**
 * Order Controller - Manages order operations
 * Refactored with Lombok for cleaner code
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final MockPaymentService mockPaymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        // Reserve inventory for 15 minutes by default
        // (The service will call warehouse reservation internally next step)
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderDto> getAllOrders() {
        log.debug("Getting all orders");
        return orderService.getAllOrders();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public List<OrderDto> getOrdersByUser(
            @PathVariable Integer userId,
            jakarta.servlet.http.HttpServletRequest request) {
        
        // Extract userId from JWT (set by JwtAuthenticationFilter)
        Long currentUserId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");
        
        log.info("GET /orders/user/{} - JWT Auth: userId={}, username={}, role={}", 
                userId, currentUserId, username, role);
        
        // ✅ SECURITY: Verify user can only access their own orders (unless ADMIN)
        if (currentUserId != null && !userId.equals(currentUserId.intValue())) {
            log.warn("SECURITY: User {} attempted to access orders of user {}", currentUserId, userId);
            throw new AccessDeniedException(currentUserId, "orders", userId);
        }
        
        return orderService.getOrdersByUser(userId);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public OrderDto getOrder(@PathVariable Integer orderId) {
        log.debug("Getting order: {}", orderId);
        return orderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public OrderDto cancelOrder(
            @PathVariable Integer orderId,
            jakarta.servlet.http.HttpServletRequest request) {
        // Extract userId from JWT
        Long currentUserId = (Long) request.getAttribute("userId");
        log.info("User {} cancelling order: {}", currentUserId, orderId);
        
        // ✅ SECURITY FIX: Verify order belongs to user
        OrderDto order = orderService.getOrder(orderId);
        if (!order.getUserId().equals(currentUserId.intValue())) {
            log.warn("SECURITY: User {} attempted to cancel order {} belonging to user {}", 
                currentUserId, orderId, order.getUserId());
            throw new AccessDeniedException(currentUserId, "order", orderId);
        }
        
        return orderService.cancelOrder(orderId);
    }

    @PostMapping("/{orderId}/return-request")
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public OrderDto requestReturn(
            @PathVariable Integer orderId,
            @RequestHeader(value = "X-User-Id", required = true) Long currentUserId){
        log.info("User {} requesting return for order: {}", currentUserId, orderId);
        
        // ✅ SECURITY FIX: Verify order belongs to user
        OrderDto order = orderService.getOrder(orderId);
        if (!order.getUserId().equals(currentUserId.intValue())) {
            log.warn("SECURITY: User {} attempted to return order {} belonging to user {}", 
                currentUserId, orderId, order.getUserId());
            throw new AccessDeniedException(currentUserId, "order", orderId);
        }
        
        return orderService.requestReturn(orderId);
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDto refundOrder(
            @PathVariable Integer orderId,
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @RequestHeader(value = "X-Username", required = false) String adminUsername){
        
        // Log admin action for audit trail
        log.warn("ADMIN ACTION: User {} ({}) refunding order {}", adminUsername, adminId, orderId);
        
        return orderService.refundOrder(orderId);
    }

    @PostMapping("/{orderId}/payment-session")
    public Map<String, Object> createPaymentSession(@PathVariable Integer orderId) {
        log.info("Creating payment session for order: {}", orderId);
        return mockPaymentService.createSession(orderId);
    }

    // Webhook endpoint (simulated)
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestParam Integer orderId,
            @RequestParam String signature,
            @RequestBody(required = false) String payload) {
        if (!mockPaymentService.verifySignature(orderId, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        OrderDto updated = orderService.markOrderPaid(orderId, payload);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Get order statistics for a user
     */
    @GetMapping("/stats/user/{userId}")
    public Map<String, Object> getUserOrderStats(@PathVariable Integer userId) {
        log.debug("Getting order stats for user: {}", userId);
        List<OrderDto> orders = orderService.getOrdersByUser(userId);
        return Map.of(
            "totalOrders", orders.size(),
            "completedOrders", orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count(),
            "pendingOrders", orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count(),
            "cancelledOrders", orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count()
        );
    }

    /**
     * Get order statistics for a shop
     */
    @GetMapping("/stats/shop")
    public OrderStatsDTO getShopOrderStats(@RequestParam Long shopId) {
        log.debug("Getting order stats for shop: {}", shopId);
        return orderService.getShopOrderStats(shopId);
    }
    
    /**
     * Get sold count for a product
     */
    @GetMapping("/product/{productId}/sold-count")
    public Integer getProductSoldCount(@PathVariable Long productId) {
        log.debug("Getting sold count for product: {}", productId);
        return orderService.getProductSoldCount(productId);
    }
    
    /**
     * Get order statistics for current user (authenticated)
     */
    @GetMapping("/stats")
    public Map<String, Object> getMyOrderStats(@RequestHeader("X-User-Id") Long userId) {
        log.debug("Getting order stats for authenticated user: {}", userId);
        List<OrderDto> orders = orderService.getOrdersByUser(userId.intValue());
        return Map.of(
            "totalOrders", orders.size(),
            "completedOrders", orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count(),
            "pendingOrders", orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count(),
            "cancelledOrders", orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count()
        );
    }
    
    /**
     * Get order status history
     */
    @GetMapping("/{orderId}/status-history")
    public List<Map<String, Object>> getOrderStatusHistory(@PathVariable Integer orderId) {
        log.debug("Getting status history for order: {}", orderId);
        try {
            OrderDto order = orderService.getOrder(orderId);
            // Return simple history for now
            return List.of(
                Map.of(
                    "status", order.getStatus(),
                    "timestamp", order.getCreatedAt(),
                    "note", "Order " + order.getStatus().toLowerCase()
                )
            );
        } catch (Exception e) {
            log.warn("Order {} not found, returning empty history", orderId);
            // Return empty list if order not found
            return List.of();
        }
    }
}
