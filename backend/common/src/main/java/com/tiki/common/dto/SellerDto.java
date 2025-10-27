package com.tiki.common.dto;

import lombok.Data;

@Data
public class SellerDto {
    private Long id;
    private Long userId;
    private String shopName;
    private String phone;
    private String address;
    private String returnPolicy;
    private String shippingPolicy;
}
