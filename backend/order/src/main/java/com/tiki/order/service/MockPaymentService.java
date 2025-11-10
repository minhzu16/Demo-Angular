package com.tiki.order.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MockPaymentService {
    public Map<String, Object> createSession(Integer orderId) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
        map.put("sessionId", "sess_" + orderId);
        map.put("paymentUrl", "https://example.com/pay/" + orderId);
        return map;
    }

    public boolean verifySignature(Integer orderId, String signature) {
        return signature != null && !signature.isBlank();
    }
}


