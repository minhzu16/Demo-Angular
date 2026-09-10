package com.tiki.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8084}")
public interface PaymentClient {

    @PostMapping("/api/v1/payments/order/{orderId}/refund")
    void refundPayment(@PathVariable("orderId") Integer orderId);
}
