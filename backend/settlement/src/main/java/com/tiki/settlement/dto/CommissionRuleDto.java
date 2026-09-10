package com.tiki.settlement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRuleDto {
    private Long id;
    private Long categoryId; // Null for platform default

    @NotNull(message = "Tỷ lệ hoa hồng không được để trống")
    @DecimalMin(value = "0.00", message = "Tỷ lệ hoa hồng tối thiểu là 0%")
    @DecimalMax(value = "100.00", message = "Tỷ lệ hoa hồng tối đa là 100%")
    private BigDecimal commissionRate;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
}
