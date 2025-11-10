package com.tiki.cart.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private Integer productId;
    private Integer variantId;
    private Integer quantity;
    private Double priceSnapshot;
}
