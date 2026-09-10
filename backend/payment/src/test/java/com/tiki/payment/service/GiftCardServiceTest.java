package com.tiki.payment.service;

import com.tiki.payment.dto.GiftCardPurchaseRequest;
import com.tiki.payment.dto.GiftCardResponseDto;
import com.tiki.payment.entity.GiftCardEntity;
import com.tiki.payment.repository.GiftCardRepository;
import com.tiki.payment.repository.GiftCardTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftCardServiceTest {

    @Mock
    private GiftCardRepository giftCardRepository;

    @Mock
    private GiftCardTransactionRepository giftCardTransactionRepository;

    @Mock
    private StoreCreditService storeCreditService;

    @InjectMocks
    private GiftCardService giftCardService;

    private GiftCardEntity activeCard;

    @BeforeEach
    void setUp() {
        activeCard = GiftCardEntity.builder()
                .id(1L)
                .code("GIFT-AAAA-BBBB-CCCC")
                .initialBalance(new BigDecimal("200000.00"))
                .currentBalance(new BigDecimal("200000.00"))
                .purchasedByUserId(10L)
                .status(GiftCardEntity.GiftCardStatus.ACTIVE)
                .isReloadable(true)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    void testPurchaseGiftCard() {
        GiftCardPurchaseRequest req = GiftCardPurchaseRequest.builder()
                .amount(new BigDecimal("500000.00"))
                .recipientEmail("friend@example.com")
                .recipientName("Friend")
                .personalMessage("Happy Birthday!")
                .validityDays(365)
                .build();

        when(giftCardRepository.save(any(GiftCardEntity.class))).thenAnswer(inv -> {
            GiftCardEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        GiftCardResponseDto res = giftCardService.purchaseGiftCard(10L, req);

        assertNotNull(res);
        assertEquals(10L, res.getId());
        assertEquals(new BigDecimal("500000.00"), res.getInitialBalance());
        assertTrue(res.getCode().startsWith("GIFT-"));
        assertEquals(GiftCardEntity.GiftCardStatus.ACTIVE, res.getStatus());
        verify(giftCardTransactionRepository).save(any());
    }

    @Test
    void testRedeemGiftCard_Success() {
        when(giftCardRepository.findByCode("GIFT-AAAA-BBBB-CCCC")).thenReturn(Optional.of(activeCard));
        when(giftCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GiftCardResponseDto res = giftCardService.redeemGiftCard("GIFT-AAAA-BBBB-CCCC", 20L);

        assertEquals(BigDecimal.ZERO, res.getCurrentBalance());
        assertEquals(GiftCardEntity.GiftCardStatus.REDEEMED, res.getStatus());
        verify(storeCreditService).addCredit(eq(20L), eq(new BigDecimal("200000.00")), any(), eq("GIFT-AAAA-BBBB-CCCC"), any());
        verify(giftCardTransactionRepository).save(any());
    }

    @Test
    void testApplyGiftCardAtCheckout_Success() {
        when(giftCardRepository.findByCode("GIFT-AAAA-BBBB-CCCC")).thenReturn(Optional.of(activeCard));
        when(giftCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GiftCardResponseDto res = giftCardService.applyGiftCardAtCheckout("GIFT-AAAA-BBBB-CCCC", new BigDecimal("50000.00"), 101);

        assertEquals(new BigDecimal("150000.00"), res.getCurrentBalance());
        assertEquals(GiftCardEntity.GiftCardStatus.ACTIVE, res.getStatus());
        verify(giftCardTransactionRepository).save(any());
    }

    @Test
    void testApplyGiftCardAtCheckout_ExceedsBalance_Throws() {
        when(giftCardRepository.findByCode("GIFT-AAAA-BBBB-CCCC")).thenReturn(Optional.of(activeCard));

        assertThrows(IllegalStateException.class, () ->
                giftCardService.applyGiftCardAtCheckout("GIFT-AAAA-BBBB-CCCC", new BigDecimal("300000.00"), 101));
    }

    @Test
    void testReloadGiftCard_Success() {
        when(giftCardRepository.findByCode("GIFT-AAAA-BBBB-CCCC")).thenReturn(Optional.of(activeCard));
        when(giftCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GiftCardResponseDto res = giftCardService.reloadGiftCard("GIFT-AAAA-BBBB-CCCC", new BigDecimal("100000.00"));

        assertEquals(new BigDecimal("300000.00"), res.getCurrentBalance());
        verify(giftCardTransactionRepository).save(any());
    }

    @Test
    void testGetGiftCard_Expired_Throws() {
        activeCard.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(giftCardRepository.findByCode("GIFT-AAAA-BBBB-CCCC")).thenReturn(Optional.of(activeCard));

        assertThrows(IllegalStateException.class, () ->
                giftCardService.getGiftCardByCode("GIFT-AAAA-BBBB-CCCC"));
    }
}
