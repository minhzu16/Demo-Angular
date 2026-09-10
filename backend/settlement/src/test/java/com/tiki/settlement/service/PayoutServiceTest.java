package com.tiki.settlement.service;

import com.tiki.settlement.entity.SellerPayoutEntity;
import com.tiki.settlement.entity.SellerSettlementEntity;
import com.tiki.settlement.repository.SellerPayoutRepository;
import com.tiki.settlement.repository.SellerSettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private SellerPayoutRepository sellerPayoutRepository;

    @Mock
    private SellerSettlementRepository sellerSettlementRepository;

    @InjectMocks
    private PayoutService payoutService;

    @Test
    void testInitiatePayout_Success() {
        SellerSettlementEntity s1 = SellerSettlementEntity.builder()
                .id(1L)
                .orderAmount(new BigDecimal("1000000.00"))
                .commissionAmount(new BigDecimal("50000.00"))
                .sellerPayoutAmount(new BigDecimal("950000.00"))
                .status(SellerSettlementEntity.SettlementStatus.CALCULATED)
                .build();

        when(sellerSettlementRepository.findByShopIdAndSettlementPeriod(10L, "2027-01"))
                .thenReturn(List.of(s1));
        when(sellerPayoutRepository.save(any(SellerPayoutEntity.class)))
                .thenAnswer(inv -> {
                    SellerPayoutEntity p = inv.getArgument(0);
                    p.setId(50L);
                    return p;
                });

        SellerPayoutEntity payout = payoutService.initiatePayout(10L, "2027-01", "123456789", "Vietcombank", "Nguyen Van A");

        assertNotNull(payout);
        assertEquals(50L, payout.getId());
        assertEquals(new BigDecimal("950000.00"), payout.getTotalPayout());
        assertEquals(SellerPayoutEntity.PayoutStatus.PENDING, payout.getStatus());
        assertEquals(SellerSettlementEntity.SettlementStatus.APPROVED, s1.getStatus());
    }

    @Test
    void testInitiatePayout_ThrowsIfNoSettlements() {
        when(sellerSettlementRepository.findByShopIdAndSettlementPeriod(10L, "2027-01"))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () ->
                payoutService.initiatePayout(10L, "2027-01", "123", "Bank", "Holder"));
    }

    @Test
    void testInitiatePayout_ThrowsIfAlreadyPaid() {
        SellerSettlementEntity s1 = SellerSettlementEntity.builder()
                .status(SellerSettlementEntity.SettlementStatus.PAID)
                .build();

        when(sellerSettlementRepository.findByShopIdAndSettlementPeriod(10L, "2027-01"))
                .thenReturn(List.of(s1));

        assertThrows(IllegalStateException.class, () ->
                payoutService.initiatePayout(10L, "2027-01", "123", "Bank", "Holder"));
    }

    @Test
    void testApprovePayout_Success() {
        SellerPayoutEntity payout = SellerPayoutEntity.builder()
                .id(1L)
                .status(SellerPayoutEntity.PayoutStatus.PENDING)
                .build();

        when(sellerPayoutRepository.findById(1L)).thenReturn(Optional.of(payout));
        when(sellerPayoutRepository.save(any(SellerPayoutEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerPayoutEntity updated = payoutService.approvePayout(1L);
        assertEquals(SellerPayoutEntity.PayoutStatus.PROCESSING, updated.getStatus());
    }

    @Test
    void testConfirmPayout_Success() {
        SellerPayoutEntity payout = SellerPayoutEntity.builder()
                .id(1L)
                .shopId(10L)
                .settlementPeriod("2027-01")
                .status(SellerPayoutEntity.PayoutStatus.PROCESSING)
                .build();

        SellerSettlementEntity s1 = SellerSettlementEntity.builder()
                .status(SellerSettlementEntity.SettlementStatus.APPROVED)
                .build();

        when(sellerPayoutRepository.findById(1L)).thenReturn(Optional.of(payout));
        when(sellerPayoutRepository.save(any(SellerPayoutEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sellerSettlementRepository.findByShopIdAndSettlementPeriod(10L, "2027-01"))
                .thenReturn(List.of(s1));

        SellerPayoutEntity confirmed = payoutService.confirmPayout(1L, "TX-99999");

        assertEquals(SellerPayoutEntity.PayoutStatus.COMPLETED, confirmed.getStatus());
        assertEquals("TX-99999", confirmed.getTransactionRef());
        assertNotNull(confirmed.getCompletedAt());
        assertEquals(SellerSettlementEntity.SettlementStatus.PAID, s1.getStatus());
        assertNotNull(s1.getPaidAt());
    }
}
