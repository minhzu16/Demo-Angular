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

import com.tiki.payment.entity.PaymentEventEntity;
import com.tiki.payment.repository.PaymentEventRepository;
import com.tiki.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

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
        log.info("Received SePay webhook payload");

        // 1. Fail-closed signature verification (Vulnerability 3.5)
        if (sepayWebhookSecret == null || sepayWebhookSecret.isBlank()) {
            log.error("SECURITY: sepay.webhook-secret is unconfigured or empty. Rejecting webhook (Fail-Closed).");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret unconfigured");
        }

        String expectedKey = "Apikey " + sepayWebhookSecret.trim();
        byte[] expectedBytes = expectedKey.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = authHeader != null ? authHeader.trim().getBytes(StandardCharsets.UTF_8) : new byte[0];

        // Constant-time comparison to prevent timing attacks
        if (authHeader == null || !MessageDigest.isEqual(providedBytes, expectedBytes)) {
            log.warn("SECURITY: Unauthorized SePay webhook attempt - invalid API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            // 2. Extract provider transaction id for unique idempotency constraint
            String providerTxnId = null;
            if (payload.get("id") != null) {
                providerTxnId = String.valueOf(payload.get("id"));
            } else if (payload.get("referenceCode") != null) {
                providerTxnId = String.valueOf(payload.get("referenceCode"));
            }

            if (providerTxnId == null || providerTxnId.isBlank()) {
                providerTxnId = "TXN_" + System.currentTimeMillis();
            }

            // Check if already processed in idempotent ledger
            if (paymentEventRepository.existsByProviderAndProviderTxnId("SEPAY", providerTxnId)) {
                log.info("SePay webhook with txnId {} already processed. Returning idempotent OK.", providerTxnId);
                return ResponseEntity.ok("already_processed");
            }

            // 3. Extract content and orderId
            String content = payload.get("content") != null ? String.valueOf(payload.get("content")) : "";
            if (!content.toUpperCase().contains("DH")) {
                log.warn("SePay webhook content lacks order reference 'DH': {}", content);
                return ResponseEntity.badRequest().body("missing_order_ref");
            }

            int dhIndex = content.toUpperCase().indexOf("DH");
            String orderIdStr = content.substring(dhIndex + 2).replaceAll("[^0-9]", "");
            if (orderIdStr.isEmpty()) {
                log.warn("Failed to extract numeric orderId from content: {}", content);
                return ResponseEntity.badRequest().body("invalid_order_id");
            }
            Integer orderId = Integer.parseInt(orderIdStr);

            // 4. Strict Amount Verification
            Object transferAmountObj = payload.get("transferAmount");
            if (transferAmountObj == null) {
                transferAmountObj = payload.get("amount");
            }
            if (transferAmountObj == null) {
                log.warn("SePay webhook missing transferAmount for order {}", orderId);
                return ResponseEntity.badRequest().body("missing_transfer_amount");
            }

            BigDecimal transferAmount = new BigDecimal(String.valueOf(transferAmountObj));
            PaymentDto payment = paymentService.getPaymentInfoByOrderId(orderId);

            if (payment.getAmount() == null || transferAmount.compareTo(payment.getAmount()) != 0) {
                log.warn("Amount mismatch in SePay webhook for order {}: Expected {}, Received {}",
                        orderId, payment.getAmount(), transferAmount);
                try {
                    paymentEventRepository.save(PaymentEventEntity.builder()
                            .provider("SEPAY")
                            .providerTxnId(providerTxnId)
                            .orderId(orderId)
                            .amount(transferAmount)
                            .status("AMOUNT_MISMATCH")
                            .rawPayload(payload.toString())
                            .build());
                } catch (Exception ignored) {}
                return ResponseEntity.badRequest().body("amount_mismatch");
            }

            // 5. Idempotent Atomic Conditional Update at Database level
            int updatedRows = paymentRepository.updatePaymentStatusConditional(orderId, "COMPLETED", providerTxnId);
            if (updatedRows > 0) {
                paymentService.updatePaymentStatusByOrderId(orderId, "COMPLETED");
                log.info("Order {} payment successfully COMPLETED via verified SePay webhook (txnId={})", orderId, providerTxnId);
            } else {
                log.info("Order {} was already marked COMPLETED or not PENDING. Conditional update skipped.", orderId);
            }

            // 6. Record unique event in audit ledger
            try {
                paymentEventRepository.save(PaymentEventEntity.builder()
                        .provider("SEPAY")
                        .providerTxnId(providerTxnId)
                        .orderId(orderId)
                        .amount(transferAmount)
                        .status("COMPLETED")
                        .rawPayload(payload.toString())
                        .build());
            } catch (DataIntegrityViolationException ex) {
                log.info("Duplicate webhook event concurrent insert caught for txnId: {}", providerTxnId);
            }

            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.badRequest().body("error");
        }
    }
}
