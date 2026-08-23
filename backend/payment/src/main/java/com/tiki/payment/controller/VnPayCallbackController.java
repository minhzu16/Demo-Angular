package com.tiki.payment.controller;

import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.service.PaymentService;
import com.tiki.payment.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class VnPayCallbackController {

    private final PaymentService paymentService;

    @Value("${vnp.hashSecret}")
    private String vnp_HashSecret;

    /**
     * Handle VNPay Callback (Return URL)
     * This is where user is redirected after payment
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<Map<String, Object>> vnpayCallback(HttpServletRequest request) {
        log.info("VNPay Callback received");
        
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = fields.get("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        
        // Verify signature
        String signValue = generateSignature(fields);
        
        boolean isValid = signValue.equals(vnp_SecureHash);
        String vnp_ResponseCode = fields.get("vnp_ResponseCode");
        String vnp_TxnRef = fields.get("vnp_TxnRef");
        Integer orderId = Integer.parseInt(vnp_TxnRef.split("_")[0]);

        if (isValid && "00".equals(vnp_ResponseCode)) {
            paymentService.updatePaymentStatusByOrderId(orderId, "SUCCESS");
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "orderId", orderId));
        } else {
            paymentService.updatePaymentStatusByOrderId(orderId, "FAILED");
            return ResponseEntity.ok(Map.of("status", "FAILED", "orderId", orderId, "code", vnp_ResponseCode));
        }
    }

    private String generateSignature(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    log.error("Encoding error", e);
                }
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return VnPayUtil.hmacSHA512(vnp_HashSecret, sb.toString());
    }
}
