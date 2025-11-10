package com.tiki.order.service;

import com.tiki.order.dto.CreateOrderRequest;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.dto.OrderStatsDTO;
import com.tiki.order.dto.PaymentInfoDTO;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import com.tiki.order.client.UserClient;
import com.tiki.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private UserClient userClient;
    
    @Autowired
    private OrderRepository orderRepository;
    public OrderDto createOrder(CreateOrderRequest request) {
        // Validate user via common-service
        if (request.getUserId() != null) {
            try {
                userClient.getUser(request.getUserId().longValue());
            } catch (Exception ex) {
                throw new IllegalArgumentException("ID người dùng không hợp lệ: " + request.getUserId());
            }
        }
        OrderDto dto = new OrderDto();
        dto.setId(1);
        dto.setUserId(request.getUserId());
        dto.setStatus("CREATED");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setTotalAmount(BigDecimal.ZERO);
        return dto;
    }

    public OrderDto getOrder(Integer orderId) {
        OrderDto dto = new OrderDto();
        dto.setId(orderId);
        dto.setStatus("CREATED");
        return dto;
    }

    public OrderDto cancelOrder(Integer orderId) {
        return updateStatus(orderId, OrderEntity.OrderStatus.CANCELLED);
    }

    public OrderDto requestReturn(Integer orderId){
        return updateStatus(orderId, OrderEntity.OrderStatus.RETURN_REQUESTED);
    }

    public OrderDto refundOrder(Integer orderId){
        return updateStatus(orderId, OrderEntity.OrderStatus.REFUNDED);
    }

    public List<OrderDto> getAllOrders() {
        return new ArrayList<>();
    }

    public List<OrderDto> getOrdersByUser(Integer userId) {
        return new ArrayList<>();
    }

    public OrderDto updateStatus(Integer orderId, OrderEntity.OrderStatus status) {
        OrderDto dto = new OrderDto();
        dto.setId(orderId);
        dto.setStatus(status.name());
        return dto;
    }

    @Autowired
    private InvoiceService invoiceService;

    public OrderDto markOrderPaid(Integer orderId, String providerPayload) {
        // generate invoice mock amount zero
        try {
            invoiceService.issueInvoice(orderId, java.math.BigDecimal.ZERO);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        OrderDto dto = new OrderDto();
        dto.setId(orderId);
        dto.setStatus("PAID");
        return dto;
    }

    /**
     * Get payment information for an order
     */
    public PaymentInfoDTO getPaymentInfo(Integer orderId) {
        log.info("Getting payment info for order: {}", orderId);
        
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        
        return PaymentInfoDTO.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .build();
    }

    /**
     * Confirm COD payment (seller confirms receiving cash)
     */
    public PaymentInfoDTO confirmCODPayment(Integer orderId) {
        log.info("Confirming COD payment for order: {}", orderId);
        
        // 1. Get order from DB
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        
        // 2. Validate order is COD
        if (!order.isCOD()) {
            throw new RuntimeException("Order không phải là COD payment");
        }
        
        // 3. Validate payment status is PENDING
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment status phải là PENDING. Hiện tại: " + order.getPaymentStatus());
        }
        
        // 4. Mark as PAID
        order.markAsPaid();
        
        // 5. Save to DB
        order = orderRepository.save(order);
        
        log.info("COD payment confirmed for order: {}", orderId);
        
        return PaymentInfoDTO.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .paymentNote("COD payment confirmed by seller")
                .build();
    }

    /**
     * Update payment status from Payment Service
     * Called by payment service after payment gateway callback
     */
    public PaymentInfoDTO updatePaymentStatus(Integer orderId, PaymentStatus paymentStatus, String transactionId) {
        log.info("Updating payment status for order: {} to {}", orderId, paymentStatus);
        
        // 1. Get order from DB
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        
        // 2. Update payment status
        order.setPaymentStatus(paymentStatus);
        
        // 3. If paid, set paid timestamp
        if (paymentStatus == PaymentStatus.PAID) {
            order.setPaidAt(LocalDateTime.now());
        }
        
        // 4. Save to DB
        order = orderRepository.save(order);
        
        log.info("Payment status updated for order: {} - Status: {}, Transaction: {}", 
                 orderId, paymentStatus, transactionId);
        
        return PaymentInfoDTO.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paidAt(order.getPaidAt())
                .paymentNote("Payment updated by payment service - Transaction: " + transactionId)
                .build();
    }
    
    /**
     * Validate payment method for order creation
     */
    public void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Phương thức thanh toán là bắt buộc");
        }
        
        // For now, only COD is supported
        if (paymentMethod != PaymentMethod.COD) {
            throw new IllegalArgumentException(
                "Phương thức thanh toán " + paymentMethod + " chưa được hỗ trợ. Hiện chỉ có COD."
            );
        }
    }
    
    /**
     * Get order statistics for a shop
     */
    public OrderStatsDTO getShopOrderStats(Long shopId) {
        log.info("Getting order stats for shop: {}", shopId);
        
        // Calculate start of today
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        
        // Get statistics from repository
        Integer todayOrders = orderRepository.countTodayOrders(startOfDay);
        BigDecimal todayRevenue = orderRepository.calculateTodayRevenue(startOfDay);
        Integer pendingOrders = orderRepository.countPendingOrders();
        
        // Get total stats
        Long totalOrders = orderRepository.count();
        
        return OrderStatsDTO.builder()
                .shopId(shopId)
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .pendingOrders(pendingOrders)
                .totalOrders(totalOrders.intValue())
                .totalRevenue(BigDecimal.ZERO)
                .build();
    }
    
    /**
     * Get sold count for a product
     * Note: This is a placeholder returning total orders count
     * In real implementation, need to sum quantities from order_items table
     */
    public Integer getProductSoldCount(Long productId) {
        log.info("Getting sold count for product: {}", productId);
        
        Integer totalOrders = orderRepository.getProductSoldCount();
        
        // Mock: assume each product sold in 10% of orders
        return totalOrders != null ? (int)(totalOrders * 0.1) : 0;
    }
}
