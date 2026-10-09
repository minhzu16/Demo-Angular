package com.tiki.payment.service;

import com.tiki.payment.dto.StoreCreditResponseDto;
import com.tiki.payment.dto.StoreCreditTransactionDto;
import com.tiki.payment.entity.StoreCreditEntity;
import com.tiki.payment.entity.StoreCreditTransactionEntity;
import com.tiki.payment.repository.StoreCreditRepository;
import com.tiki.payment.repository.StoreCreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreCreditService {

    private final StoreCreditRepository storeCreditRepository;
    private final StoreCreditTransactionRepository storeCreditTransactionRepository;

    public StoreCreditResponseDto getBalance(Long userId) {
        StoreCreditEntity entity = storeCreditRepository.findByUserId(userId)
                .orElse(StoreCreditEntity.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .updatedAt(LocalDateTime.now())
                        .build());

        return StoreCreditResponseDto.builder()
                .userId(userId)
                .balance(entity.getBalance())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Transactional
    public StoreCreditTransactionDto addCredit(
            Long userId,
            BigDecimal amount,
            StoreCreditTransactionEntity.TransactionType type,
            String refId,
            String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp vào Store Credit phải lớn hơn 0");
        }

        StoreCreditEntity entity = storeCreditRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> StoreCreditEntity.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build());

        BigDecimal newBalance = entity.getBalance().add(amount);
        entity.setBalance(newBalance);
        storeCreditRepository.save(entity);

        StoreCreditTransactionEntity tx = StoreCreditTransactionEntity.builder()
                .userId(userId)
                .type(type)
                .amount(amount)
                .referenceId(refId)
                .balanceAfter(newBalance)
                .note(note)
                .build();

        StoreCreditTransactionEntity savedTx = storeCreditTransactionRepository.save(tx);
        log.info("Added {} Store Credit for user {}, new balance={}", amount, userId, newBalance);
        return toTxDto(savedTx);
    }

    @Transactional
    public StoreCreditTransactionDto deductCredit(
            Long userId,
            BigDecimal amount,
            String refId,
            String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán từ Store Credit phải lớn hơn 0");
        }

        StoreCreditEntity entity = storeCreditRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Ví Store Credit chưa được kích hoạt hoặc không có số dư"));

        if (entity.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư Store Credit không đủ. Số dư hiện có: " +
                    entity.getBalance() + "đ, yêu cầu: " + amount + "đ");
        }

        BigDecimal newBalance = entity.getBalance().subtract(amount);
        entity.setBalance(newBalance);
        storeCreditRepository.save(entity);

        StoreCreditTransactionEntity tx = StoreCreditTransactionEntity.builder()
                .userId(userId)
                .type(StoreCreditTransactionEntity.TransactionType.ORDER_PAYMENT)
                .amount(amount.negate())
                .referenceId(refId)
                .balanceAfter(newBalance)
                .note(note)
                .build();

        StoreCreditTransactionEntity savedTx = storeCreditTransactionRepository.save(tx);
        log.info("Deducted {} Store Credit for user {}, new balance={}", amount, userId, newBalance);
        return toTxDto(savedTx);
    }

    public Page<StoreCreditTransactionDto> getTransactionHistory(Long userId, Pageable pageable) {
        return storeCreditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toTxDto);
    }

    private StoreCreditTransactionDto toTxDto(StoreCreditTransactionEntity e) {
        return StoreCreditTransactionDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .type(e.getType())
                .amount(e.getAmount())
                .referenceId(e.getReferenceId())
                .balanceAfter(e.getBalanceAfter())
                .note(e.getNote())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
