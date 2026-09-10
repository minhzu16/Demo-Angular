package com.tiki.live.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class LivePinProductRequest {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    private String productName;
    private BigDecimal originalPrice;

    @NotNull(message = "Giá ưu đãi live không được để trống")
    @DecimalMin("1000.00")
    private BigDecimal livePrice;

    private Integer stockLimit;
}
