package com.tiki.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POActionDto {
    private String reason;
    private Integer convertedOrderId;
    private BigDecimal creditLimit;
    private Integer paymentTermDays;
}
