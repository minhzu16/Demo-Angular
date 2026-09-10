package com.tiki.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "settlement-service", url = "${SETTLEMENT_SERVICE_URL:http://localhost:8094}")
public interface SettlementClient {

    class OrderSettlementRequest {
        private Integer orderId;
        private Long shopId;
        private Long categoryId;
        private BigDecimal orderAmount;

        public OrderSettlementRequest() {}

        public OrderSettlementRequest(Integer orderId, Long shopId, Long categoryId, BigDecimal orderAmount) {
            this.orderId = orderId;
            this.shopId = shopId;
            this.categoryId = categoryId;
            this.orderAmount = orderAmount;
        }

        public Integer getOrderId() { return orderId; }
        public void setOrderId(Integer orderId) { this.orderId = orderId; }
        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public BigDecimal getOrderAmount() { return orderAmount; }
        public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
    }

    @PostMapping("/api/v1/settlement/calculate")
    void calculateSettlement(@RequestBody OrderSettlementRequest request);
}
