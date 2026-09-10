package com.tiki.live.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuickBuyRequest {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @NotNull
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    @Builder.Default
    private Integer quantity = 1;
}
