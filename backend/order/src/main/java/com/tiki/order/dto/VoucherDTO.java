package com.tiki.order.dto;

import com.tiki.order.entity.VoucherEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherDTO {
    private Integer id;
    private String code;
    private VoucherEntity.DiscountType type;
    private BigDecimal value;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer remainingUsage;
    private Boolean isActive;
    private Boolean isExpired;
    private Boolean hasStarted;
    private LocalDateTime createdAt;
}
