package com.tiki.b2b.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class B2BPriceTierDto {
    private Long id;

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng tối thiểu không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu phải từ 1")
    private Integer minQuantity;

    private Integer maxQuantity; // Null = unlimited

    @NotNull(message = "Đơn giá sỉ không được để trống")
    @DecimalMin("1000.00")
    private BigDecimal unitPrice;

    private Boolean isActive;
}
