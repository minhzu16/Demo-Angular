package com.tiki.order.controller;

import com.tiki.order.dto.PaymentInfoDTO;
import com.tiki.order.dto.PaymentRequestDto;
import com.tiki.order.dto.PaymentResponseDto;
import com.tiki.order.service.OrderService;
import com.tiki.order.service.StripePaymentService;
import com.tiki.order.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    @Autowired
    private StripePaymentService stripePaymentService;
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentRequestDto request) {
        try {
            PaymentResponseDto response = stripePaymentService.createPaymentIntent(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create payment: " + e.getMessage());
        }
    }
    
    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<PaymentResponseDto> confirmPayment(@PathVariable String paymentIntentId) {
        try {
            PaymentResponseDto response = stripePaymentService.confirmPayment(paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new BadRequestException("Failed to confirm payment: " + e.getMessage());
        }
    }
    
    @PostMapping("/cancel/{paymentIntentId}")
    public ResponseEntity<PaymentResponseDto> cancelPayment(@PathVariable String paymentIntentId) {
        try {
            PaymentResponseDto response = stripePaymentService.cancelPayment(paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new BadRequestException("Failed to cancel payment: " + e.getMessage());
        }
    }
    
    @GetMapping("/{paymentIntentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable String paymentIntentId) {
        try {
            PaymentResponseDto response = stripePaymentService.getPayment(paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get payment: " + e.getMessage());
        }
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, 
                                               @RequestHeader("Stripe-Signature") String signature) {
        try {
            stripePaymentService.handleWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            throw new BadRequestException("Webhook processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Get payment information for an order
     * GET /api/v1/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentInfoDTO> getOrderPaymentInfo(@PathVariable Integer orderId) {
        try {
            PaymentInfoDTO paymentInfo = orderService.getPaymentInfo(orderId);
            return ResponseEntity.ok(paymentInfo);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get payment info: " + e.getMessage());
        }
    }
    
    /**
     * Confirm COD payment (seller confirms receiving cash)
     * PUT /api/v1/payments/order/{orderId}/confirm-cod
     */
    @PutMapping("/order/{orderId}/confirm-cod")
    public ResponseEntity<PaymentInfoDTO> confirmCODPayment(@PathVariable Integer orderId) {
        try {
            PaymentInfoDTO paymentInfo = orderService.confirmCODPayment(orderId);
            return ResponseEntity.ok(paymentInfo);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            throw new BadRequestException("Failed to confirm COD payment: " + e.getMessage());
        }
    }
    
    /**
     * Update payment status (called by Payment Service)
     * PUT /api/v1/payments/order/{orderId}/status
     */
    @PutMapping("/order/{orderId}/status")
    public ResponseEntity<PaymentInfoDTO> updatePaymentStatus(
            @PathVariable Integer orderId,
            @RequestParam String paymentStatus,
            @RequestParam(required = false) String transactionId) {
        try {
            com.tiki.order.enums.PaymentStatus status = com.tiki.order.enums.PaymentStatus.valueOf(paymentStatus);
            PaymentInfoDTO paymentInfo = orderService.updatePaymentStatus(orderId, status, transactionId);
            return ResponseEntity.ok(paymentInfo);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment status: " + paymentStatus);
        } catch (Exception e) {
            throw new BadRequestException("Failed to update payment status: " + e.getMessage());
        }
    }
}
