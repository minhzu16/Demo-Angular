package com.tiki.cart.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartDto {
    private Integer id;
    private Integer userId;
    private String sessionId;
    private Integer totalItems;
    private Double totalAmount;
    private List<CartItemDto> cartItems;
}
