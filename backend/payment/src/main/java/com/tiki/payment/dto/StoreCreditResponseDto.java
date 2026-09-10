package com.tiki.payment.dto;

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
public class StoreCreditResponseDto {
    private Long userId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}
