package com.tiki.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8084}")
public interface PaymentClient {

    @PostMapping("/api/v1/payments/order/{orderId}/refund")
    void refundPayment(@PathVariable("orderId") Integer orderId);

    @PostMapping("/api/v1/gift-cards/apply")
    default void applyGiftCard(@org.springframework.web.bind.annotation.RequestBody Object request) {}

    @PostMapping("/api/v1/store-credit/deduct")
    default void deductStoreCredit(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @org.springframework.web.bind.annotation.RequestBody Object request) {}
}
