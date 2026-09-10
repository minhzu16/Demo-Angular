package com.tiki.settlement.service;

import com.tiki.settlement.entity.SellerPayoutEntity;
import com.tiki.settlement.entity.SellerSettlementEntity;
import com.tiki.settlement.repository.SellerPayoutRepository;
import com.tiki.settlement.repository.SellerSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayoutService {

    private final SellerPayoutRepository sellerPayoutRepository;
    private final SellerSettlementRepository sellerSettlementRepository;

    @Transactional
    public SellerPayoutEntity initiatePayout(Long shopId, String period, String bankAccount, String bankName, String accountHolder) {
        List<SellerSettlementEntity> settlements = sellerSettlementRepository.findByShopIdAndSettlementPeriod(shopId, period);
        if (settlements.isEmpty()) {
            throw new IllegalStateException("Không có giao dịch quyết toán nào trong kỳ " + period + " cho shop " + shopId);
        }

        BigDecimal totalOrder = BigDecimal.ZERO;
        BigDecimal totalComm = BigDecimal.ZERO;
        BigDecimal totalPayout = BigDecimal.ZERO;

        for (SellerSettlementEntity s : settlements) {
            if (s.getStatus() == SellerSettlementEntity.SettlementStatus.PAID) {
                throw new IllegalStateException("Kỳ quyết toán " + period + " của shop " + shopId + " đã được chi trả trước đó.");
            }
            totalOrder = totalOrder.add(s.getOrderAmount());
            totalComm = totalComm.add(s.getCommissionAmount());
            totalPayout = totalPayout.add(s.getSellerPayoutAmount());
            s.setStatus(SellerSettlementEntity.SettlementStatus.APPROVED);
        }
        sellerSettlementRepository.saveAll(settlements);

        SellerPayoutEntity payout = SellerPayoutEntity.builder()
                .shopId(shopId)
                .settlementPeriod(period)
                .periodStart(LocalDate.now().withDayOfMonth(1))
                .periodEnd(LocalDate.now())
                .totalOrderAmount(totalOrder)
                .totalCommission(totalComm)
                .totalPayout(totalPayout)
                .payoutMethod(SellerPayoutEntity.PayoutMethod.BANK_TRANSFER)
                .bankAccountNumber(bankAccount)
                .bankName(bankName)
                .accountHolderName(accountHolder)
                .status(SellerPayoutEntity.PayoutStatus.PENDING)
                .build();

        SellerPayoutEntity saved = sellerPayoutRepository.save(payout);
        log.info("Initiated payout id={} for shopId={}, period={}, amount={}", saved.getId(), shopId, period, totalPayout);
        return saved;
    }

    @Transactional
    public SellerPayoutEntity approvePayout(Long payoutId) {
        SellerPayoutEntity payout = sellerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu chi trả id=" + payoutId));

        if (payout.getStatus() != SellerPayoutEntity.PayoutStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu chi trả không ở trạng thái PENDING. Trạng thái hiện tại: " + payout.getStatus());
        }

        payout.setStatus(SellerPayoutEntity.PayoutStatus.PROCESSING);
        SellerPayoutEntity saved = sellerPayoutRepository.save(payout);
        log.info("Approved payout id={} to PROCESSING", payoutId);
        return saved;
    }

    @Transactional
    public SellerPayoutEntity confirmPayout(Long payoutId, String transactionRef) {
        SellerPayoutEntity payout = sellerPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu chi trả id=" + payoutId));

        if (payout.getStatus() == SellerPayoutEntity.PayoutStatus.COMPLETED) {
            throw new IllegalStateException("Yêu cầu chi trả id=" + payoutId + " đã hoàn tất trước đó.");
        }

        payout.setStatus(SellerPayoutEntity.PayoutStatus.COMPLETED);
        payout.setTransactionRef(transactionRef);
        payout.setCompletedAt(LocalDateTime.now());
        SellerPayoutEntity saved = sellerPayoutRepository.save(payout);

        // Update all related settlements to PAID
        List<SellerSettlementEntity> settlements = sellerSettlementRepository
                .findByShopIdAndSettlementPeriod(payout.getShopId(), payout.getSettlementPeriod());
        for (SellerSettlementEntity s : settlements) {
            s.setStatus(SellerSettlementEntity.SettlementStatus.PAID);
            s.setPaidAt(LocalDateTime.now());
        }
        sellerSettlementRepository.saveAll(settlements);

        log.info("Confirmed payout id={} with transactionRef={}, completed {} settlements",
                payoutId, transactionRef, settlements.size());
        return saved;
    }

    public List<SellerPayoutEntity> getPayoutHistory(Long shopId) {
        return sellerPayoutRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }
}
