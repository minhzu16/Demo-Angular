package com.tiki.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDto {
    private Integer productId;
    private Integer quantity;
    private BigDecimal priceSnapshot;
}
