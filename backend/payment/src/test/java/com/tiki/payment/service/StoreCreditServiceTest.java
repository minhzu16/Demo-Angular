package com.tiki.payment.service;

import com.tiki.payment.dto.StoreCreditResponseDto;
import com.tiki.payment.dto.StoreCreditTransactionDto;
import com.tiki.payment.entity.StoreCreditEntity;
import com.tiki.payment.entity.StoreCreditTransactionEntity;
import com.tiki.payment.repository.StoreCreditRepository;
import com.tiki.payment.repository.StoreCreditTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreCreditServiceTest {

    @Mock
    private StoreCreditRepository storeCreditRepository;

    @Mock
    private StoreCreditTransactionRepository storeCreditTransactionRepository;

    @InjectMocks
    private StoreCreditService storeCreditService;

    private StoreCreditEntity existingWallet;

    @BeforeEach
    void setUp() {
        existingWallet = StoreCreditEntity.builder()
                .id(1L)
                .userId(10L)
                .balance(new BigDecimal("150000.00"))
                .build();
    }

    @Test
    void testGetBalance_ExistingWallet() {
        when(storeCreditRepository.findByUserId(10L)).thenReturn(Optional.of(existingWallet));

        StoreCreditResponseDto res = storeCreditService.getBalance(10L);

        assertEquals(10L, res.getUserId());
        assertEquals(new BigDecimal("150000.00"), res.getBalance());
    }

    @Test
    void testGetBalance_NewUser_ReturnsZero() {
        when(storeCreditRepository.findByUserId(99L)).thenReturn(Optional.empty());

        StoreCreditResponseDto res = storeCreditService.getBalance(99L);

        assertEquals(99L, res.getUserId());
        assertEquals(BigDecimal.ZERO, res.getBalance());
    }

    @Test
    void testAddCredit_Success() {
        when(storeCreditRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(existingWallet));
        when(storeCreditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storeCreditTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StoreCreditTransactionDto tx = storeCreditService.addCredit(
                10L,
                new BigDecimal("50000.00"),
                StoreCreditTransactionEntity.TransactionType.REFUND_CREDIT,
                "RMA-12345",
                "Hoàn tiền qua Store Credit"
        );

        assertNotNull(tx);
        assertEquals(new BigDecimal("50000.00"), tx.getAmount());
        assertEquals(new BigDecimal("200000.00"), tx.getBalanceAfter());
        assertEquals("RMA-12345", tx.getReferenceId());
        assertEquals(new BigDecimal("200000.00"), existingWallet.getBalance());
    }

    @Test
    void testDeductCredit_Success() {
        when(storeCreditRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(existingWallet));
        when(storeCreditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storeCreditTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StoreCreditTransactionDto tx = storeCreditService.deductCredit(
                10L,
                new BigDecimal("50000.00"),
                "ORDER-101",
                "Thanh toán đơn hàng"
        );

        assertNotNull(tx);
        assertEquals(new BigDecimal("-50000.00"), tx.getAmount());
        assertEquals(new BigDecimal("100000.00"), tx.getBalanceAfter());
        assertEquals(new BigDecimal("100000.00"), existingWallet.getBalance());
    }

    @Test
    void testDeductCredit_InsufficientBalance_Throws() {
        when(storeCreditRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(existingWallet));

        assertThrows(IllegalStateException.class, () ->
                storeCreditService.deductCredit(10L, new BigDecimal("500000.00"), "ORDER-101", "Thanh toán"));
    }
}
