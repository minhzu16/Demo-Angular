package com.tiki.payment.controller;

import com.tiki.payment.dto.CreatePaymentRequest;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @Value("${sepay.webhook-secret:}")
    private String sepayWebhookSecret;

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

    @PostMapping("/order/{orderId}/refund")
    public ResponseEntity<PaymentDto> refundPayment(@PathVariable Integer orderId) {
        log.info("Request refund for order: {}", orderId);
        return ResponseEntity.ok(paymentService.refundPayment(orderId));
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<String> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody java.util.Map<String, Object> payload) {
        log.info("Received SePay webhook: {}", payload);
        try {
            // 1. Signature/API Key Verification
            if (sepayWebhookSecret != null && !sepayWebhookSecret.isBlank()) {
                String expectedKey = "Apikey " + sepayWebhookSecret.trim();
                if (authHeader == null || (!authHeader.equalsIgnoreCase(expectedKey) && !authHeader.contains(sepayWebhookSecret))) {
                    log.warn("Unauthorized SePay webhook attempt with header: {}", authHeader);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
                }
            }

            // 2. Extract content and orderId
            String content = payload.get("content") != null ? String.valueOf(payload.get("content")) : "";
            if (content.toUpperCase().contains("DH")) {
                int dhIndex = content.toUpperCase().indexOf("DH");
                String orderIdStr = content.substring(dhIndex + 2).replaceAll("[^0-9]", "");
                if (!orderIdStr.isEmpty()) {
                    Integer orderId = Integer.parseInt(orderIdStr);

                    // 3. Idempotency Check
                    PaymentDto payment = paymentService.getPaymentInfoByOrderId(orderId);
                    if ("COMPLETED".equalsIgnoreCase(payment.getPaymentStatus())) {
                        log.info("Order {} payment is already COMPLETED. Skipping duplicate webhook.", orderId);
                        return ResponseEntity.ok("already_processed");
                    }

                    // 4. Amount Verification
                    Object transferAmountObj = payload.get("transferAmount");
                    if (transferAmountObj == null) {
                        transferAmountObj = payload.get("amount");
                    }
                    if (transferAmountObj != null) {
                        BigDecimal transferAmount = new BigDecimal(String.valueOf(transferAmountObj));
                        if (payment.getAmount() != null && transferAmount.compareTo(payment.getAmount()) < 0) {
                            log.warn("Underpaid SePay webhook for order {}: Expected {}, Received {}", 
                                    orderId, payment.getAmount(), transferAmount);
                            return ResponseEntity.badRequest().body("underpaid");
                        }
                    }

                    paymentService.updatePaymentStatusByOrderId(orderId, "COMPLETED");
                    log.info("Successfully updated order {} to COMPLETED via verified SePay webhook", orderId);
                }
            }
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.badRequest().body("error");
        }
    }
}
