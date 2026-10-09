package com.tiki.b2b.controller;

import com.tiki.b2b.dto.*;
import com.tiki.b2b.service.B2BService;
import com.tiki.b2b.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Identity comes only from the gateway-validated X-User-Id / X-User-Role headers. The headers are required:
 * they used to default to user 1 (and even admin 999 for company verification), so any anonymous request
 * acted as a company owner or as the platform admin.
 */
@RestController
@RequestMapping("/api/v1/b2b")
@RequiredArgsConstructor
public class B2BController {

    private final B2BService b2bService;
    private final PurchaseOrderService purchaseOrderService;

    private static boolean isAdmin(String role) {
        return role != null && role.toUpperCase().contains("ADMIN");
    }

    /** Company data and purchase orders are visible to the company's active members and platform admins only. */
    private void requireMemberOrAdmin(Long companyId, Long userId, String role) {
        if (!isAdmin(role) && !b2bService.isActiveMember(companyId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc doanh nghiệp này");
        }
    }

    private static void requireAdmin(String role) {
        if (!isAdmin(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới thực hiện được thao tác này");
        }
    }

    @PostMapping("/companies")
    public ResponseEntity<CompanyResponseDto> registerCompany(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CompanyRegistrationDto dto) {
        return ResponseEntity.ok(b2bService.registerCompany(dto, userId));
    }

    @PostMapping("/companies/{id}/verify")
    public ResponseEntity<CompanyResponseDto> verifyCompany(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody(required = false) POActionDto action) {
        requireAdmin(role);
        BigDecimal creditLimit = action != null && action.getCreditLimit() != null
                ? action.getCreditLimit() : new BigDecimal("50000000.00");
        Integer paymentTerms = action != null && action.getPaymentTermDays() != null
                ? action.getPaymentTermDays() : 30;
        return ResponseEntity.ok(b2bService.verifyCompany(id, adminId, creditLimit, paymentTerms));
    }

    @GetMapping("/companies/{id}")
    public ResponseEntity<CompanyResponseDto> getCompany(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireMemberOrAdmin(id, userId, role);
        return ResponseEntity.ok(b2bService.getCompany(id));
    }

    @PostMapping("/prices")
    public ResponseEntity<B2BPriceTierDto> addPriceTier(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody B2BPriceTierDto dto) {
        requireAdmin(role);
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
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PurchaseOrderCreateRequest req) {
        return ResponseEntity.ok(purchaseOrderService.createPO(userId, req));
    }

    @GetMapping("/purchase-orders/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> getPOById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        PurchaseOrderResponseDto po = purchaseOrderService.getPOById(id);
        requireMemberOrAdmin(po.getCompanyId(), userId, role);
        return ResponseEntity.ok(po);
    }

    @PostMapping("/purchase-orders/{id}/approve")
    public ResponseEntity<PurchaseOrderResponseDto> approvePO(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long approverId) {
        return ResponseEntity.ok(purchaseOrderService.approvePO(id, approverId));
    }

    @PostMapping("/purchase-orders/{id}/reject")
    public ResponseEntity<PurchaseOrderResponseDto> rejectPO(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long approverId,
            @RequestBody(required = false) POActionDto action) {
        String reason = action != null && action.getReason() != null ? action.getReason() : "Từ chối duyệt đơn mua hàng";
        return ResponseEntity.ok(purchaseOrderService.rejectPO(id, approverId, reason));
    }

    @PostMapping("/purchase-orders/{id}/convert")
    public ResponseEntity<PurchaseOrderResponseDto> convertToOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody POActionDto action) {
        if (action == null || action.getConvertedOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu convertedOrderId");
        }
        PurchaseOrderResponseDto po = purchaseOrderService.getPOById(id);
        requireMemberOrAdmin(po.getCompanyId(), userId, role);
        return ResponseEntity.ok(purchaseOrderService.convertToOrder(id, userId, action.getConvertedOrderId()));
    }

    @GetMapping("/purchase-orders/company/{companyId}")
    public ResponseEntity<Page<PurchaseOrderResponseDto>> getCompanyPOs(
            @PathVariable Long companyId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PageableDefault(size = 10) Pageable pageable) {
        requireMemberOrAdmin(companyId, userId, role);
        return ResponseEntity.ok(purchaseOrderService.getCompanyPOs(companyId, pageable));
    }
}
