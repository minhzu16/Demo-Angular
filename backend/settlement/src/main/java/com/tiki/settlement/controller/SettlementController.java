package com.tiki.settlement.controller;

import com.tiki.settlement.client.ShopClient;
import com.tiki.settlement.dto.*;
import com.tiki.settlement.entity.SellerPayoutEntity;
import com.tiki.settlement.entity.SellerSettlementEntity;
import com.tiki.settlement.service.CommissionRuleService;
import com.tiki.settlement.service.PayoutService;
import com.tiki.settlement.service.SettlementCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Money endpoints. Identity/role come only from the gateway-validated X-User-Id / X-User-Role headers:
 *  - commission rules (write), payout approval/confirmation: ADMIN only;
 *  - a shop's summary / payout history / payout request: that shop's own seller (resolved through shop-service,
 *    shop ids are not user ids) or ADMIN;
 *  - /calculate is service-to-service (blocked at the gateway).
 * Previously every endpoint was open: anyone could change commission rates, request a payout for any shop to a
 * bank account of their choice (the defaults were a fixed sample account) and approve/confirm it themselves.
 */
@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final CommissionRuleService commissionRuleService;
    private final SettlementCalculationService settlementCalculationService;
    private final PayoutService payoutService;
    private final ShopClient shopClient;

    private static boolean isAdmin(String role) {
        return role != null && role.toUpperCase().contains("ADMIN");
    }

    private static void requireAdmin(String role) {
        if (!isAdmin(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới thực hiện được thao tác này");
        }
    }

    private void requireShopAccess(Long shopId, Long callerId, String role) {
        if (isAdmin(role)) {
            return;
        }
        boolean owns = false;
        if (callerId != null) {
            try {
                ShopClient.ShopRef shop = shopClient.getShopBySeller(callerId);
                owns = shop != null && shopId != null && shopId.equals(shop.id());
            } catch (Exception e) {
                log.warn("Could not verify shop ownership for seller {}: {}", callerId, e.getMessage()); // fail closed
            }
        }
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể xem/yêu cầu quyết toán của cửa hàng mình");
        }
    }

    @GetMapping("/rules")
    public ResponseEntity<List<CommissionRuleDto>> getAllRules() {
        return ResponseEntity.ok(commissionRuleService.getAllRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<CommissionRuleDto> createRule(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody CommissionRuleDto dto) {
        requireAdmin(role);
        return ResponseEntity.ok(commissionRuleService.createRule(dto));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id) {
        requireAdmin(role);
        commissionRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /** Called by order-service when an order is delivered (blocked at the gateway). */
    @PostMapping("/calculate")
    public ResponseEntity<SellerSettlementEntity> calculateSettlement(@Valid @RequestBody OrderSettlementRequest request) {
        return ResponseEntity.ok(settlementCalculationService.calculateSettlement(request));
    }

    @GetMapping("/shop/{shopId}/summary")
    public ResponseEntity<SettlementSummaryDto> getShopSummary(
            @PathVariable Long shopId,
            @RequestParam(required = false) String period,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireShopAccess(shopId, callerId, role);
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return ResponseEntity.ok(settlementCalculationService.generatePeriodSummary(shopId, period));
    }

    /** Bank details are mandatory — no sample-account defaults. */
    @PostMapping("/shop/{shopId}/payout")
    public ResponseEntity<SellerPayoutEntity> initiatePayout(
            @PathVariable Long shopId,
            @RequestParam(required = false) String period,
            @RequestParam String bankAccount,
            @RequestParam String bankName,
            @RequestParam String accountHolder,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireShopAccess(shopId, callerId, role);
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return ResponseEntity.ok(payoutService.initiatePayout(shopId, period, bankAccount, bankName, accountHolder));
    }

    @GetMapping("/shop/{shopId}/history")
    public ResponseEntity<List<SellerPayoutEntity>> getPayoutHistory(
            @PathVariable Long shopId,
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireShopAccess(shopId, callerId, role);
        return ResponseEntity.ok(payoutService.getPayoutHistory(shopId));
    }

    @PostMapping("/payouts/{id}/approve")
    public ResponseEntity<SellerPayoutEntity> approvePayout(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(payoutService.approvePayout(id));
    }

    @PostMapping("/payouts/{id}/confirm")
    public ResponseEntity<SellerPayoutEntity> confirmPayout(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody PayoutConfirmDto dto) {
        requireAdmin(role);
        return ResponseEntity.ok(payoutService.confirmPayout(id, dto.getTransactionRef()));
    }
}
