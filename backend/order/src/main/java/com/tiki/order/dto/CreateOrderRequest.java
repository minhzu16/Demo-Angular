package com.tiki.order.dto;

import com.tiki.order.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest {
    private Integer userId;

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemDto> items;

    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddressDto shippingAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public ShippingAddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    private String voucherCode;
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    private Integer usePoints;
    public Integer getUsePoints() { return usePoints; }
    public void setUsePoints(Integer usePoints) { this.usePoints = usePoints; }

    public static class OrderItemDto {
        @NotNull(message = "Product ID is required")
        private Integer productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;

        private BigDecimal unitPrice;
        // Alias to support frontend "price" field
        private BigDecimal price;
        private String productName;
        private String imageUrl;

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice != null ? unitPrice : price; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public static class ShippingAddressDto {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        private String province;
        private String district;
        private String ward;

        @NotBlank(message = "Street address is required")
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


