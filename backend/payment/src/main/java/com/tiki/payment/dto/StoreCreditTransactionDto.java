package com.tiki.payment.dto;

import com.tiki.payment.entity.StoreCreditTransactionEntity;
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
public class StoreCreditTransactionDto {
    private Long id;
    private Long userId;
    private StoreCreditTransactionEntity.TransactionType type;
    private BigDecimal amount;
    private String referenceId;
    private BigDecimal balanceAfter;
    private String note;
    private LocalDateTime createdAt;
}
