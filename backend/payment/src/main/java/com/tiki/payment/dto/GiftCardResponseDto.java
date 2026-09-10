package com.tiki.payment.dto;

import com.tiki.payment.entity.GiftCardEntity;
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
public class GiftCardResponseDto {
    private Long id;
    private String code;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private Long purchasedByUserId;
    private String recipientEmail;
    private String recipientName;
    private String personalMessage;
    private GiftCardEntity.GiftCardStatus status;
    private Boolean isReloadable;
    private LocalDateTime activatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
