package com.tiki.order.controller;

import com.tiki.order.dto.RmaActionDto;
import com.tiki.order.dto.RmaCreateRequest;
import com.tiki.order.dto.RmaResponseDto;
import com.tiki.order.service.RmaService;
import com.tiki.order.service.ShopOwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Return/exchange (RMA) lifecycle. Identity and role come from the gateway-validated headers and are required —
 * the X-User-Id headers used to default to user 1 / staff 999 and nothing checked roles, so any signed-in buyer
 * could approve, inspect, refund or exchange any RMA, and read anyone's.
 *
 *  - buyer: create, list own, add return tracking (service checks the RMA is theirs), read own
 *  - seller: approve / reject RMAs of their own shop (ownership resolved through shop-service)
 *  - admin: receive, inspect, refund, exchange, list all
 */
@RestController
@RequestMapping("/api/v1/rma")
@RequiredArgsConstructor
public class RmaController {

    private final RmaService rmaService;
    private final ShopOwnershipService shopOwnershipService;

    private static boolean isAdmin(String role) {
        return role != null && role.toUpperCase().contains("ADMIN");
    }

    private static void requireAdmin(String role) {
        if (!isAdmin(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới thực hiện được thao tác này");
        }
    }

    private void requireShopAccess(Long shopId, Long callerId, String role) {
        if (!isAdmin(role) && !shopOwnershipService.ownsShop(callerId, shopId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Yêu cầu này không thuộc cửa hàng của bạn");
        }
    }

    @PostMapping
    public ResponseEntity<RmaResponseDto> createRma(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RmaCreateRequest req) {
        return ResponseEntity.ok(rmaService.createRma(userId, req));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<RmaResponseDto>> getMyRmas(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(rmaService.getMyRmas(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RmaResponseDto> getRmaById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        RmaResponseDto rma = rmaService.getRmaById(id);
        boolean owner = userId.equals(rma.getUserId());
        if (!owner) {
            requireShopAccess(rma.getShopId(), userId, role);
        }
        return ResponseEntity.ok(rma);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Page<RmaResponseDto>> getShopRmas(
            @PathVariable Long shopId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PageableDefault(size = 10) Pageable pageable) {
        requireShopAccess(shopId, userId, role);
        return ResponseEntity.ok(rmaService.getShopRmas(shopId, pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RmaResponseDto> approveRma(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody(required = false) RmaActionDto action) {
        requireShopAccess(rmaService.getRmaById(id).getShopId(), sellerId, role);
        String note = action != null ? action.getNote() : "Chấp nhận đổi/trả";
        return ResponseEntity.ok(rmaService.sellerApproveRma(id, sellerId, note));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RmaResponseDto> rejectRma(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody(required = false) RmaActionDto action) {
        requireShopAccess(rmaService.getRmaById(id).getShopId(), sellerId, role);
        String reason = action != null ? action.getReason() : "Từ chối yêu cầu đổi/trả";
        return ResponseEntity.ok(rmaService.sellerRejectRma(id, sellerId, reason));
    }

    @PutMapping("/{id}/tracking")
    public ResponseEntity<RmaResponseDto> updateReturnTracking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody RmaActionDto action) {
        return ResponseEntity.ok(rmaService.updateReturnTracking(
                id, userId, action.getTrackingNumber(), action.getCarrier()));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<RmaResponseDto> confirmItemReceived(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody(required = false) RmaActionDto action) {
        requireAdmin(role);
        String note = action != null ? action.getNote() : "Kho đã nhận kiện hàng trả về";
        return ResponseEntity.ok(rmaService.confirmItemReceived(id, staffId, note));
    }

    @PostMapping("/{id}/inspect")
    public ResponseEntity<RmaResponseDto> submitInspection(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody RmaActionDto action) {
        requireAdmin(role);
        return ResponseEntity.ok(rmaService.submitInspection(
                id, staffId, action.getInspectionResult(), action.getNote()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RmaResponseDto> processRefund(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(rmaService.processRefund(id));
    }

    @PostMapping("/{id}/exchange")
    public ResponseEntity<RmaResponseDto> processExchange(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam Integer newOrderId) {
        requireAdmin(role);
        return ResponseEntity.ok(rmaService.processExchange(id, newOrderId));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<RmaResponseDto>> getAllRmas(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        requireAdmin(role);
        return ResponseEntity.ok(rmaService.getAllRmas(pageable));
    }
}
