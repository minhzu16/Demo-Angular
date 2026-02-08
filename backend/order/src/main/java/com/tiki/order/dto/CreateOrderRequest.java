package com.tiki.order.dto;

import com.tiki.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest {
    private Integer userId;
    private List<OrderItemDto> items;
    private ShippingAddressDto shippingAddress;
    private PaymentMethod paymentMethod;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public static class OrderItemDto {
        private Integer productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        // Alias to support frontend "price" field
        private BigDecimal price;

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice != null ? unitPrice : price; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public static class ShippingAddressDto {
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
}


