package com.tiki.settlement.service;

import com.tiki.settlement.dto.OrderSettlementRequest;
import com.tiki.settlement.dto.SettlementSummaryDto;
import com.tiki.settlement.entity.SellerSettlementEntity;
import com.tiki.settlement.repository.SellerSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementCalculationService {

    private final SellerSettlementRepository sellerSettlementRepository;
    private final CommissionRuleService commissionRuleService;

    @Transactional
    public SellerSettlementEntity calculateSettlement(OrderSettlementRequest request) {
        // Idempotency: avoid calculating twice for the same order
        Optional<SellerSettlementEntity> existing = sellerSettlementRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            log.info("Settlement already calculated for orderId={}", request.getOrderId());
            return existing.get();
        }

        BigDecimal rate = commissionRuleService.getApplicableRate(request.getCategoryId());
        BigDecimal orderAmount = request.getOrderAmount() != null ? request.getOrderAmount() : BigDecimal.ZERO;
        BigDecimal commissionAmount = orderAmount.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal sellerPayoutAmount = orderAmount.subtract(commissionAmount);

        String period = request.getSettlementPeriod();
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        SellerSettlementEntity settlement = SellerSettlementEntity.builder()
                .shopId(request.getShopId())
                .orderId(request.getOrderId())
                .orderAmount(orderAmount)
                .commissionRate(rate)
                .commissionAmount(commissionAmount)
                .sellerPayoutAmount(sellerPayoutAmount)
                .status(SellerSettlementEntity.SettlementStatus.CALCULATED)
                .settlementPeriod(period)
                .build();

        SellerSettlementEntity saved = sellerSettlementRepository.save(settlement);
        log.info("Calculated settlement for orderId={}, shopId={}, rate={}%, commission={}, payout={}",
                saved.getOrderId(), saved.getShopId(), rate, commissionAmount, sellerPayoutAmount);
        return saved;
    }

    public SettlementSummaryDto generatePeriodSummary(Long shopId, String period) {
        List<SellerSettlementEntity> settlements = sellerSettlementRepository.findByShopIdAndSettlementPeriod(shopId, period);

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalPayout = BigDecimal.ZERO;

        for (SellerSettlementEntity s : settlements) {
            totalOrderAmount = totalOrderAmount.add(s.getOrderAmount());
            totalCommission = totalCommission.add(s.getCommissionAmount());
            totalPayout = totalPayout.add(s.getSellerPayoutAmount());
        }

        String payoutStatus = "NONE";
        if (!settlements.isEmpty()) {
            boolean allPaid = settlements.stream().allMatch(s -> s.getStatus() == SellerSettlementEntity.SettlementStatus.PAID);
            boolean allApproved = settlements.stream().allMatch(s -> s.getStatus() == SellerSettlementEntity.SettlementStatus.APPROVED);
            if (allPaid) {
                payoutStatus = "PAID";
            } else if (allApproved) {
                payoutStatus = "APPROVED";
            } else {
                payoutStatus = "CALCULATED";
            }
        }

        return SettlementSummaryDto.builder()
                .shopId(shopId)
                .period(period)
                .totalOrders(settlements.size())
                .totalOrderAmount(totalOrderAmount)
                .totalCommissionAmount(totalCommission)
                .totalSellerPayoutAmount(totalPayout)
                .payoutStatus(payoutStatus)
                .build();
    }
}
