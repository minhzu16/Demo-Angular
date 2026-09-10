package com.tiki.order.dto;

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
public class RmaItemDto {
    private Long id;
    private Long rmaId;

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    private String productName;

    @NotNull(message = "Số lượng mua không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private Integer quantity;

    @NotNull(message = "Số lượng trả không được để trống")
    @Min(value = 1, message = "Số lượng trả tối thiểu là 1")
    private Integer returnQuantity;

    private BigDecimal unitPrice;
    private String imageUrls;
}
