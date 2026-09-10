package com.tiki.order.controller;

import com.tiki.order.dto.RmaActionDto;
import com.tiki.order.dto.RmaCreateRequest;
import com.tiki.order.dto.RmaResponseDto;
import com.tiki.order.service.RmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rma")
@RequiredArgsConstructor
public class RmaController {

    private final RmaService rmaService;

    @PostMapping
    public ResponseEntity<RmaResponseDto> createRma(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody RmaCreateRequest req) {
        return ResponseEntity.ok(rmaService.createRma(userId, req));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<RmaResponseDto>> getMyRmas(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(rmaService.getMyRmas(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RmaResponseDto> getRmaById(@PathVariable Long id) {
        return ResponseEntity.ok(rmaService.getRmaById(id));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Page<RmaResponseDto>> getShopRmas(
            @PathVariable Long shopId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(rmaService.getShopRmas(shopId, pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RmaResponseDto> approveRma(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId,
            @RequestBody(required = false) RmaActionDto action) {
        String note = action != null ? action.getNote() : "Chấp nhận đổi/trả";
        return ResponseEntity.ok(rmaService.sellerApproveRma(id, sellerId, note));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RmaResponseDto> rejectRma(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long sellerId,
            @RequestBody(required = false) RmaActionDto action) {
        String reason = action != null ? action.getReason() : "Từ chối yêu cầu đổi/trả";
        return ResponseEntity.ok(rmaService.sellerRejectRma(id, sellerId, reason));
    }

    @PutMapping("/{id}/tracking")
    public ResponseEntity<RmaResponseDto> updateReturnTracking(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestBody RmaActionDto action) {
        return ResponseEntity.ok(rmaService.updateReturnTracking(
                id, userId, action.getTrackingNumber(), action.getCarrier()));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<RmaResponseDto> confirmItemReceived(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "999") Long staffId,
            @RequestBody(required = false) RmaActionDto action) {
        String note = action != null ? action.getNote() : "Kho đã nhận kiện hàng trả về";
        return ResponseEntity.ok(rmaService.confirmItemReceived(id, staffId, note));
    }

    @PostMapping("/{id}/inspect")
    public ResponseEntity<RmaResponseDto> submitInspection(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "999") Long staffId,
            @RequestBody RmaActionDto action) {
        return ResponseEntity.ok(rmaService.submitInspection(
                id, staffId, action.getInspectionResult(), action.getNote()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RmaResponseDto> processRefund(@PathVariable Long id) {
        return ResponseEntity.ok(rmaService.processRefund(id));
    }

    @PostMapping("/{id}/exchange")
    public ResponseEntity<RmaResponseDto> processExchange(
            @PathVariable Long id,
            @RequestParam Integer newOrderId) {
        return ResponseEntity.ok(rmaService.processExchange(id, newOrderId));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<RmaResponseDto>> getAllRmas(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(rmaService.getAllRmas(pageable));
    }
}
