package com.tiki.payment.service;

import com.tiki.payment.dto.GiftCardPurchaseRequest;
import com.tiki.payment.dto.GiftCardResponseDto;
import com.tiki.payment.entity.GiftCardEntity;
import com.tiki.payment.entity.GiftCardTransactionEntity;
import com.tiki.payment.entity.StoreCreditTransactionEntity;
import com.tiki.payment.repository.GiftCardRepository;
import com.tiki.payment.repository.GiftCardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GiftCardService {

    private final GiftCardRepository giftCardRepository;
    private final GiftCardTransactionRepository giftCardTransactionRepository;
    private final StoreCreditService storeCreditService;

    @Transactional
    public GiftCardResponseDto purchaseGiftCard(Long userId, GiftCardPurchaseRequest req) {
        String code = generateGiftCardCode();
        int validityDays = req.getValidityDays() != null ? req.getValidityDays() : 365;
        LocalDateTime now = LocalDateTime.now();

        GiftCardEntity entity = GiftCardEntity.builder()
                .code(code)
                .initialBalance(req.getAmount())
                .currentBalance(req.getAmount())
                .purchasedByUserId(userId)
                .recipientEmail(req.getRecipientEmail())
                .recipientName(req.getRecipientName())
                .personalMessage(req.getPersonalMessage())
                .isReloadable(req.getIsReloadable() != null ? req.getIsReloadable() : true)
                .status(GiftCardEntity.GiftCardStatus.ACTIVE)
                .activatedAt(now)
                .expiresAt(now.plusDays(validityDays))
                .build();

        GiftCardEntity saved = giftCardRepository.save(entity);

        GiftCardTransactionEntity tx = GiftCardTransactionEntity.builder()
                .giftCardId(saved.getId())
                .type(GiftCardTransactionEntity.TransactionType.PURCHASE)
                .amount(req.getAmount())
                .balanceAfter(req.getAmount())
                .build();
        giftCardTransactionRepository.save(tx);

        log.info("User {} purchased gift card {} for {} VND", userId, code, req.getAmount());
        return toDto(saved);
    }

    public GiftCardResponseDto getGiftCardByCode(String code) {
        GiftCardEntity entity = getValidCard(code);
        return toDto(entity);
    }

    @Transactional
    public GiftCardResponseDto redeemGiftCard(String code, Long userId) {
        GiftCardEntity card = getValidCard(code);

        if (card.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Thẻ quà tặng không còn số dư để quy đổi");
        }

        BigDecimal amountToRedeem = card.getCurrentBalance();

        // 1. Add credit to user's store credit wallet
        storeCreditService.addCredit(
                userId,
                amountToRedeem,
                StoreCreditTransactionEntity.TransactionType.GIFT_CARD_REDEEM,
                code,
                "Quy đổi thẻ quà tặng " + code
        );

        // 2. Mark card as redeemed
        card.setCurrentBalance(BigDecimal.ZERO);
        card.setStatus(GiftCardEntity.GiftCardStatus.REDEEMED);
        GiftCardEntity saved = giftCardRepository.save(card);

        // 3. Record transaction
        GiftCardTransactionEntity tx = GiftCardTransactionEntity.builder()
                .giftCardId(card.getId())
                .type(GiftCardTransactionEntity.TransactionType.REDEEM)
                .amount(amountToRedeem.negate())
                .balanceAfter(BigDecimal.ZERO)
                .build();
        giftCardTransactionRepository.save(tx);

        log.info("User {} redeemed gift card {} for {} VND into Store Credit", userId, code, amountToRedeem);
        return toDto(saved);
    }

    @Transactional
    public GiftCardResponseDto applyGiftCardAtCheckout(String code, BigDecimal amount, Integer orderId) {
        GiftCardEntity card = getValidCard(code);

        if (card.getCurrentBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư thẻ quà tặng không đủ. Số dư hiện tại: " +
                    card.getCurrentBalance() + "đ, yêu cầu: " + amount + "đ");
        }

        BigDecimal newBalance = card.getCurrentBalance().subtract(amount);
        card.setCurrentBalance(newBalance);
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            card.setStatus(GiftCardEntity.GiftCardStatus.REDEEMED);
        }
        GiftCardEntity saved = giftCardRepository.save(card);

        GiftCardTransactionEntity tx = GiftCardTransactionEntity.builder()
                .giftCardId(card.getId())
                .type(GiftCardTransactionEntity.TransactionType.REDEEM)
                .amount(amount.negate())
                .orderId(orderId)
                .balanceAfter(newBalance)
                .build();
        giftCardTransactionRepository.save(tx);

        log.info("Applied {} VND from gift card {} to order {}", amount, code, orderId);
        return toDto(saved);
    }

    @Transactional
    public GiftCardResponseDto reloadGiftCard(String code, BigDecimal amount) {
        GiftCardEntity card = giftCardRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ quà tặng: " + code));

        if (!Boolean.TRUE.equals(card.getIsReloadable())) {
            throw new IllegalStateException("Thẻ quà tặng này không hỗ trợ nạp thêm tiền.");
        }
        if (card.getStatus() == GiftCardEntity.GiftCardStatus.CANCELLED) {
            throw new IllegalStateException("Thẻ quà tặng đã bị hủy.");
        }

        BigDecimal newBalance = card.getCurrentBalance().add(amount);
        card.setCurrentBalance(newBalance);
        card.setStatus(GiftCardEntity.GiftCardStatus.ACTIVE);
        GiftCardEntity saved = giftCardRepository.save(card);

        GiftCardTransactionEntity tx = GiftCardTransactionEntity.builder()
                .giftCardId(card.getId())
                .type(GiftCardTransactionEntity.TransactionType.RELOAD)
                .amount(amount)
                .balanceAfter(newBalance)
                .build();
        giftCardTransactionRepository.save(tx);

        log.info("Reloaded {} VND to gift card {}, new balance={}", amount, code, newBalance);
        return toDto(saved);
    }

    public Page<GiftCardResponseDto> getMyGiftCards(Long userId, Pageable pageable) {
        return giftCardRepository.findByPurchasedByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    private GiftCardEntity getValidCard(String code) {
        GiftCardEntity card = giftCardRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã thẻ quà tặng không hợp lệ: " + code));

        if (card.getStatus() == GiftCardEntity.GiftCardStatus.CANCELLED) {
            throw new IllegalStateException("Thẻ quà tặng đã bị vô hiệu hóa.");
        }
        if (card.getStatus() == GiftCardEntity.GiftCardStatus.REDEEMED && card.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Thẻ quà tặng đã được sử dụng hết.");
        }
        if (card.getExpiresAt() != null && card.getExpiresAt().isBefore(LocalDateTime.now())) {
            card.setStatus(GiftCardEntity.GiftCardStatus.EXPIRED);
            giftCardRepository.save(card);
            throw new IllegalStateException("Thẻ quà tặng đã hết hạn sử dụng vào ngày: " + card.getExpiresAt());
        }

        return card;
    }

    private String generateGiftCardCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return "GIFT-" + raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12);
    }

    private GiftCardResponseDto toDto(GiftCardEntity e) {
        return GiftCardResponseDto.builder()
                .id(e.getId())
                .code(e.getCode())
                .initialBalance(e.getInitialBalance())
                .currentBalance(e.getCurrentBalance())
                .purchasedByUserId(e.getPurchasedByUserId())
                .recipientEmail(e.getRecipientEmail())
                .recipientName(e.getRecipientName())
                .personalMessage(e.getPersonalMessage())
                .status(e.getStatus())
                .isReloadable(e.getIsReloadable())
                .activatedAt(e.getActivatedAt())
                .expiresAt(e.getExpiresAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
