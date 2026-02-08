package com.tiki.order.dto;

import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {
    private Integer id;
    private Integer userId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private ShippingAddress shippingAddress;
    private List<Item> items;

    public static class Item {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }

    public static class ShippingAddress {
        private String fullName;
        private String phoneNumber;
        private String province;
        private String district;
        private String ward;
        private String street;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}


