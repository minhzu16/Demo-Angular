package com.tiki.cart.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartDto {
    private Integer id;
    private Integer userId;
    private String sessionId;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private List<CartItemDto> cartItems;
}
