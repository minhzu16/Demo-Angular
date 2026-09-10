package com.tiki.settlement.service;

import com.tiki.settlement.dto.OrderSettlementRequest;
import com.tiki.settlement.dto.SettlementSummaryDto;
import com.tiki.settlement.entity.SellerSettlementEntity;
import com.tiki.settlement.repository.SellerSettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementCalculationServiceTest {

    @Mock
    private SellerSettlementRepository sellerSettlementRepository;

    @Mock
    private CommissionRuleService commissionRuleService;

    @InjectMocks
    private SettlementCalculationService settlementCalculationService;

    @Test
    void testCalculateSettlement_NewOrder() {
        OrderSettlementRequest request = OrderSettlementRequest.builder()
                .orderId(101)
                .shopId(5L)
                .categoryId(2L)
                .orderAmount(new BigDecimal("1000000.00"))
                .settlementPeriod("2027-01")
                .build();

        when(sellerSettlementRepository.findByOrderId(101)).thenReturn(Optional.empty());
        when(commissionRuleService.getApplicableRate(2L)).thenReturn(new BigDecimal("5.00")); // 5%
        when(sellerSettlementRepository.save(any(SellerSettlementEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SellerSettlementEntity result = settlementCalculationService.calculateSettlement(request);

        assertNotNull(result);
        assertEquals(101, result.getOrderId());
        assertEquals(5L, result.getShopId());
        assertEquals(new BigDecimal("1000000.00"), result.getOrderAmount());
        assertEquals(new BigDecimal("5.00"), result.getCommissionRate());
        assertEquals(new BigDecimal("50000.00"), result.getCommissionAmount()); // 5% of 1,000,000 = 50,000
        assertEquals(new BigDecimal("950000.00"), result.getSellerPayoutAmount()); // 1,000,000 - 50,000 = 950,000
        assertEquals(SellerSettlementEntity.SettlementStatus.CALCULATED, result.getStatus());
        assertEquals("2027-01", result.getSettlementPeriod());
    }

    @Test
    void testCalculateSettlement_Idempotent_AlreadyExists() {
        SellerSettlementEntity existing = SellerSettlementEntity.builder()
                .id(1L)
                .orderId(102)
                .shopId(5L)
                .orderAmount(new BigDecimal("500000.00"))
                .commissionAmount(new BigDecimal("25000.00"))
                .sellerPayoutAmount(new BigDecimal("475000.00"))
                .build();

        when(sellerSettlementRepository.findByOrderId(102)).thenReturn(Optional.of(existing));

        OrderSettlementRequest request = OrderSettlementRequest.builder()
                .orderId(102)
                .shopId(5L)
                .orderAmount(new BigDecimal("500000.00"))
                .build();

        SellerSettlementEntity result = settlementCalculationService.calculateSettlement(request);

        assertEquals(existing, result);
        verify(sellerSettlementRepository, never()).save(any());
    }

    @Test
    void testGeneratePeriodSummary() {
        SellerSettlementEntity s1 = SellerSettlementEntity.builder()
                .orderAmount(new BigDecimal("1000000.00"))
                .commissionAmount(new BigDecimal("50000.00"))
                .sellerPayoutAmount(new BigDecimal("950000.00"))
                .status(SellerSettlementEntity.SettlementStatus.CALCULATED)
                .build();

        SellerSettlementEntity s2 = SellerSettlementEntity.builder()
                .orderAmount(new BigDecimal("2000000.00"))
                .commissionAmount(new BigDecimal("100000.00"))
                .sellerPayoutAmount(new BigDecimal("1900000.00"))
                .status(SellerSettlementEntity.SettlementStatus.CALCULATED)
                .build();

        when(sellerSettlementRepository.findByShopIdAndSettlementPeriod(5L, "2027-01"))
                .thenReturn(List.of(s1, s2));

        SettlementSummaryDto summary = settlementCalculationService.generatePeriodSummary(5L, "2027-01");

        assertNotNull(summary);
        assertEquals(5L, summary.getShopId());
        assertEquals("2027-01", summary.getPeriod());
        assertEquals(2, summary.getTotalOrders());
        assertEquals(new BigDecimal("3000000.00"), summary.getTotalOrderAmount());
        assertEquals(new BigDecimal("150000.00"), summary.getTotalCommissionAmount());
        assertEquals(new BigDecimal("2850000.00"), summary.getTotalSellerPayoutAmount());
        assertEquals("CALCULATED", summary.getPayoutStatus());
    }
}
