package com.tiki.b2b.controller;

import com.tiki.b2b.dto.*;
import com.tiki.b2b.service.B2BService;
import com.tiki.b2b.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/b2b")
@RequiredArgsConstructor
public class B2BController {

    private final B2BService b2bService;
    private final PurchaseOrderService purchaseOrderService;

    @PostMapping("/companies")
    public ResponseEntity<CompanyResponseDto> registerCompany(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody CompanyRegistrationDto dto) {
        return ResponseEntity.ok(b2bService.registerCompany(dto, userId));
    }

    @PostMapping("/companies/{id}/verify")
    public ResponseEntity<CompanyResponseDto> verifyCompany(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "999") Long adminId,
            @RequestBody(required = false) POActionDto action) {
        BigDecimal creditLimit = action != null ? action.getCreditLimit() : new BigDecimal("50000000.00");
        Integer paymentTerms = action != null ? action.getPaymentTermDays() : 30;
        return ResponseEntity.ok(b2bService.verifyCompany(id, adminId, creditLimit, paymentTerms));
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<CompanyResponseDto> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(b2bService.getCompany(id));
    }

    @PostMapping("/prices")
    public ResponseEntity<B2BPriceTierDto> addPriceTier(@Valid @RequestBody B2BPriceTierDto dto) {
        return ResponseEntity.ok(b2bService.addPriceTier(dto));
    }

    @GetMapping("/prices/{productId}")
    public ResponseEntity<List<B2BPriceTierDto>> getPriceTiers(@PathVariable Long productId) {
        return ResponseEntity.ok(b2bService.getPriceTiers(productId));
    }

    @GetMapping("/prices/{productId}/quote")
    public ResponseEntity<BigDecimal> quotePrice(
            @PathVariable Long productId,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "100000.00") BigDecimal defaultPrice) {
        return ResponseEntity.ok(b2bService.getB2BPrice(productId, quantity, defaultPrice));
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrderResponseDto> createPO(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody PurchaseOrderCreateRequest req) {
        return ResponseEntity.ok(purchaseOrderService.createPO(userId, req));
    }

    @GetMapping("/purchase-orders/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> getPOById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPOById(id));
    }

    @PostMapping("/purchase-orders/{id}/approve")
    public ResponseEntity<PurchaseOrderResponseDto> approvePO(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long approverId) {
        return ResponseEntity.ok(purchaseOrderService.approvePO(id, approverId));
    }

    @PostMapping("/purchase-orders/{id}/reject")
    public ResponseEntity<PurchaseOrderResponseDto> rejectPO(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long approverId,
            @RequestBody(required = false) POActionDto action) {
        String reason = action != null ? action.getReason() : "Từ chối duyệt đơn mua hàng";
        return ResponseEntity.ok(purchaseOrderService.rejectPO(id, approverId, reason));
    }

    @PostMapping("/purchase-orders/{id}/convert")
    public ResponseEntity<PurchaseOrderResponseDto> convertToOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestBody POActionDto action) {
        Integer orderId = action != null ? action.getConvertedOrderId() : 1001;
        return ResponseEntity.ok(purchaseOrderService.convertToOrder(id, userId, orderId));
    }

    @GetMapping("/purchase-orders/company/{companyId}")
    public ResponseEntity<Page<PurchaseOrderResponseDto>> getCompanyPOs(
            @PathVariable Long companyId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getCompanyPOs(companyId, pageable));
    }
}
