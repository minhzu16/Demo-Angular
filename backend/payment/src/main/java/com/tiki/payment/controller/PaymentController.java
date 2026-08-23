package com.tiki.payment.controller;

import com.tiki.payment.dto.CreatePaymentRequest;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Request create payment for order id: {}", request.getOrderId());
        return ResponseEntity.ok(paymentService.createPayment(request));
    }

    @GetMapping("/order/{orderId}/info")
    public ResponseEntity<PaymentDto> getPaymentInfo(@PathVariable Integer orderId) {
        log.info("Request get payment info for order: {}", orderId);
        return ResponseEntity.ok(paymentService.getPaymentInfoByOrderId(orderId));
    }

    @PostMapping("/confirm/{intentId}/{status}")
    public ResponseEntity<PaymentDto> confirmPayment(@PathVariable String intentId, @PathVariable String status) {
        log.info("Request confirm payment for intent: {}, status: {}", intentId, status);
        return ResponseEntity.ok(paymentService.confirmPaymentByIntentId(intentId, status));
    }
    
    @PutMapping("/order/{orderId}/status")
    public ResponseEntity<PaymentDto> updatePaymentStatus(
            @PathVariable Integer orderId,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        log.info("Request update payment status for order: {}, status: {}, userId: {}", orderId, status, currentUserId);
        // ✅ BUG 6 FIX: Chỉ cho phép nếu có userId hợp lệ (gateway inject)
        if (currentUserId == null) {
            log.warn("SECURITY: Attempt to update payment status without authentication");
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(paymentService.updatePaymentStatusByOrderId(orderId, status));
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<String> handleSepayWebhook(@RequestBody java.util.Map<String, Object> payload) {
        log.info("Received SePay webhook: {}", payload);
        try {
            // SePay typically sends 'content' containing the transfer description and 'transferAmount'
            String content = (String) payload.get("content");
            if (content != null && content.toUpperCase().contains("DH")) {
                // Extract orderId, assuming format DH123
                int dhIndex = content.toUpperCase().indexOf("DH");
                String orderIdStr = content.substring(dhIndex + 2).replaceAll("[^0-9]", "");
                if (!orderIdStr.isEmpty()) {
                    Integer orderId = Integer.parseInt(orderIdStr);
                    paymentService.updatePaymentStatusByOrderId(orderId, "COMPLETED");
                    log.info("Successfully updated order {} to COMPLETED via SePay webhook", orderId);
                }
            }
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.badRequest().body("error");
        }
    }
}
