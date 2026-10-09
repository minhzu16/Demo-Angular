package com.tiki.payment.controller;

import com.tiki.payment.dto.GiftCardApplyRequest;
import com.tiki.payment.dto.GiftCardPurchaseRequest;
import com.tiki.payment.dto.GiftCardReloadRequest;
import com.tiki.payment.dto.GiftCardResponseDto;
import com.tiki.payment.service.GiftCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gift-cards")
@RequiredArgsConstructor
public class GiftCardController {

    private final GiftCardService giftCardService;

    @PostMapping("/purchase")
    public ResponseEntity<GiftCardResponseDto> purchaseGiftCard(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody GiftCardPurchaseRequest req) {
        return ResponseEntity.ok(giftCardService.purchaseGiftCard(userId, req));
    }

    @GetMapping("/{code}/balance")
    public ResponseEntity<GiftCardResponseDto> getBalance(@PathVariable String code) {
        return ResponseEntity.ok(giftCardService.getGiftCardByCode(code));
    }

    @PostMapping("/redeem")
    public ResponseEntity<GiftCardResponseDto> redeemGiftCard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String code) {
        return ResponseEntity.ok(giftCardService.redeemGiftCard(code, userId));
    }

    @PostMapping("/apply")
    public ResponseEntity<GiftCardResponseDto> applyGiftCard(
            @Valid @RequestBody GiftCardApplyRequest req) {
        return ResponseEntity.ok(giftCardService.applyGiftCardAtCheckout(req.getCode(), req.getAmount(), req.getOrderId()));
    }

    @PostMapping("/{code}/reload")
    public ResponseEntity<GiftCardResponseDto> reloadGiftCard(
            @PathVariable String code,
            @Valid @RequestBody GiftCardReloadRequest req) {
        return ResponseEntity.ok(giftCardService.reloadGiftCard(code, req.getAmount()));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<GiftCardResponseDto>> getMyGiftCards(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(giftCardService.getMyGiftCards(userId, pageable));
    }
}
