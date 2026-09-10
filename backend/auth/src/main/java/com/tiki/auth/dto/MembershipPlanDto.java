package com.tiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
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
public class MembershipPlanDto {
    private Long id;

    @NotBlank(message = "Mã gói không được để trống")
    private String code;

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá tháng không được để trống")
    private BigDecimal priceMonthly;

    private BigDecimal priceYearly;
    private String currency;
    private String benefits;
    private Integer maxFreeShipPerMonth;
    private BigDecimal freeShipMaxValue;
    private Double pointsMultiplier;
    private Integer trialDays;
    private Boolean isActive;
    private Integer displayOrder;
}
