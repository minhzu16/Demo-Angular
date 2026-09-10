package com.tiki.settlement.controller;

import com.tiki.settlement.dto.*;
import com.tiki.settlement.entity.CommissionRuleEntity;
import com.tiki.settlement.entity.SellerPayoutEntity;
import com.tiki.settlement.entity.SellerSettlementEntity;
import com.tiki.settlement.service.CommissionRuleService;
import com.tiki.settlement.service.PayoutService;
import com.tiki.settlement.service.SettlementCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final CommissionRuleService commissionRuleService;
    private final SettlementCalculationService settlementCalculationService;
    private final PayoutService payoutService;

    @GetMapping("/rules")
    public ResponseEntity<List<CommissionRuleDto>> getAllRules() {
        return ResponseEntity.ok(commissionRuleService.getAllRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<CommissionRuleDto> createRule(@Valid @RequestBody CommissionRuleDto dto) {
        return ResponseEntity.ok(commissionRuleService.createRule(dto));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        commissionRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calculate")
    public ResponseEntity<SellerSettlementEntity> calculateSettlement(@Valid @RequestBody OrderSettlementRequest request) {
        return ResponseEntity.ok(settlementCalculationService.calculateSettlement(request));
    }

    @GetMapping("/shop/{shopId}/summary")
    public ResponseEntity<SettlementSummaryDto> getShopSummary(
            @PathVariable Long shopId,
            @RequestParam(required = false) String period) {
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return ResponseEntity.ok(settlementCalculationService.generatePeriodSummary(shopId, period));
    }

    @PostMapping("/shop/{shopId}/payout")
    public ResponseEntity<SellerPayoutEntity> initiatePayout(
            @PathVariable Long shopId,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "10987654321") String bankAccount,
            @RequestParam(defaultValue = "Vietcombank") String bankName,
            @RequestParam(defaultValue = "Shop Owner") String accountHolder) {
        if (period == null || period.isBlank()) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return ResponseEntity.ok(payoutService.initiatePayout(shopId, period, bankAccount, bankName, accountHolder));
    }

    @GetMapping("/shop/{shopId}/history")
    public ResponseEntity<List<SellerPayoutEntity>> getPayoutHistory(@PathVariable Long shopId) {
        return ResponseEntity.ok(payoutService.getPayoutHistory(shopId));
    }

    @PostMapping("/payouts/{id}/approve")
    public ResponseEntity<SellerPayoutEntity> approvePayout(@PathVariable Long id) {
        return ResponseEntity.ok(payoutService.approvePayout(id));
    }

    @PostMapping("/payouts/{id}/confirm")
    public ResponseEntity<SellerPayoutEntity> confirmPayout(
            @PathVariable Long id,
            @Valid @RequestBody PayoutConfirmDto dto) {
        return ResponseEntity.ok(payoutService.confirmPayout(id, dto.getTransactionRef()));
    }
}
