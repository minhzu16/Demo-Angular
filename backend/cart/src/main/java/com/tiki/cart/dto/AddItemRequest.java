package com.tiki.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddItemRequest {
    private Integer userId;
    
    @NotNull(message = "Product ID is required")
    @Min(value = 1, message = "Product ID must be positive")
    private Integer productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    // Price is ignored for security - will be fetched from ProductService
    @Deprecated
    private Double priceSnapshot;
}
