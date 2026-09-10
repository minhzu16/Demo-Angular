package com.tiki.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${order.service.url:http://localhost:8083}")
public interface OrderClient {

    @PutMapping("/api/v1/payments/order/{orderId}/status")
    void updatePaymentStatus(
            @PathVariable("orderId") Integer orderId,
            @RequestParam("paymentStatus") String paymentStatus,
            @RequestParam(value = "transactionId", required = false) String transactionId
    );
}
