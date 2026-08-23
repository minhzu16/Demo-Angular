package com.tiki.payment.service;

import com.tiki.payment.dto.CreatePaymentRequest;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.entity.PaymentEntity;
import com.tiki.payment.repository.PaymentRepository;
import com.tiki.payment.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;

    @Value("${vnp.payUrl}")
    private String vnp_PayUrl;
    @Value("${vnp.returnUrl}")
    private String vnp_ReturnUrl;
    @Value("${vnp.tmnCode}")
    private String vnp_TmnCode;
    @Value("${vnp.hashSecret}")
    private String vnp_HashSecret;

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for order ID: {}", request.getOrderId());
        
        // Find existing payment for the same order if any
        PaymentEntity existing = paymentRepository.findByOrderId(request.getOrderId()).orElse(null);
        if (existing != null) {
            log.warn("Payment already exists for order id: {}, id: {}", request.getOrderId(), existing.getId());
            return mapToDto(existing);
        }

        PaymentEntity entity = PaymentEntity.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PENDING")
                .transactionId(UUID.randomUUID().toString()) // Initial placeholder
                .build();
        
        // If Stripe, we might leave paymentIntentId empty until actual creation in Controller or here.
        
        PaymentEntity saved = paymentRepository.save(entity);
        PaymentDto dto = mapToDto(saved);

        // Generate VNPay URL if method is VNPAY
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            String paymentUrl = generateVnPayUrl(request.getOrderId(), request.getAmount());
            dto.setRedirectUrl(paymentUrl);
        } else if ("SEPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            String paymentUrl = generateSepayUrl(request.getOrderId(), request.getAmount());
            dto.setRedirectUrl(paymentUrl);
        } else if ("COD".equalsIgnoreCase(request.getPaymentMethod())) {
            dto.setRedirectUrl(""); // No redirect for COD
        }

        return dto;
    }

    private String generateSepayUrl(Integer orderId, BigDecimal amount) {
        // Generate VietQR format URL via SePay
        // Format: https://qr.sepay.vn/img?acc=YOUR_ACCOUNT_NUMBER&bank=YOUR_BANK_NAME&amount=AMOUNT&des=MEMO
        // We will use placeholders for bank account info which can be overridden via properties in real prod
        String accountNo = "0123456789"; // Thay bằng số tài khoản thật
        String bank = "MBBank"; // Thay bằng tên ngân hàng thật
        String memo = "DH" + orderId;
        return String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s", 
                             accountNo, bank, amount.intValue(), memo);
    }

    private String generateVnPayUrl(Integer orderId, BigDecimal amount) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = "Thanh toan don hang " + orderId;
        String vnp_TxnRef = orderId.toString() + "_" + System.currentTimeMillis();
        String vnp_IpAddr = "127.0.0.1";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal(100)).intValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        try {
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    //Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("VNPay Encoding error", e);
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayUtil.hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnp_PayUrl + "?" + queryUrl;
    }
    
    @Transactional(readOnly = true)
    public PaymentDto getPaymentInfoByOrderId(Integer orderId) {
        log.info("Fetching payment info for order id: {}", orderId);
        PaymentEntity existing = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));
        return mapToDto(existing);
    }
    
    @Transactional
    public PaymentDto confirmPaymentByIntentId(String intentId, String status) {
        log.info("Confirming payment for intent: {}, status: {}", intentId, status);
        PaymentEntity existing = paymentRepository.findByPaymentIntentId(intentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent id: " + intentId));
        
        existing.setPaymentStatus(status);
        return mapToDto(paymentRepository.save(existing));
    }
    
    @Transactional
    public PaymentDto updatePaymentStatusByOrderId(Integer orderId, String status) {
        PaymentEntity existing = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));
        
        existing.setPaymentStatus(status);
        return mapToDto(paymentRepository.save(existing));
    }

    private PaymentDto mapToDto(PaymentEntity entity) {
        return PaymentDto.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .paymentStatus(entity.getPaymentStatus())
                .transactionId(entity.getTransactionId())
                .paymentIntentId(entity.getPaymentIntentId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
