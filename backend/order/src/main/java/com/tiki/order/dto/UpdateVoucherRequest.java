package com.tiki.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateVoucherRequest {

    @DecimalMin(value = "0.01", message = "Value must be greater than 0")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "Min order value must be >= 0")
    private BigDecimal minOrderValue;

    private LocalDateTime endDate;

    @Min(value = 1, message = "Max usage must be at least 1")
    private Integer maxUsage;

    private Boolean isActive;
}
