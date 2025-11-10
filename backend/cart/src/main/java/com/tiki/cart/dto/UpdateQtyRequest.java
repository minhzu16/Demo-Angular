package com.tiki.cart.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQtyRequest {
    @Min(1)
    private Integer quantity;
}
