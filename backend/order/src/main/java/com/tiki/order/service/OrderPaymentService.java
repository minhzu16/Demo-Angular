package com.tiki.order.service;

import com.tiki.order.dto.OrderDto;
import com.tiki.order.dto.PaymentInfoDTO;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import com.tiki.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final InvoiceService invoiceService;
    private final OrderMapper orderMapper;

    public OrderDto markOrderPaid(Integer orderId, String providerPayload) {
        // generate invoice mock amount zero
        try {
            invoiceService.issueInvoice(orderId, java.math.BigDecimal.ZERO);
        } catch (java.io.IOException e) {
            log.error("Failed to issue invoice for order {}", orderId, e);
        }
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
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
}
