package com.tiki.order.controller;

import com.tiki.order.dto.*;
import com.tiki.order.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;
    
    /**
     * Get available vouchers for user (v1 API)
     * GET /api/v1/vouchers/available
     */
    @GetMapping("/v1/vouchers/available")
    public ResponseEntity<List<VoucherDTO>> getAvailableVouchers(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<VoucherDTO> vouchers = voucherService.getValidVouchers();
        return ResponseEntity.ok(vouchers);
    }
    
    /**
     * Get my vouchers (v1 API)
     * GET /api/v1/vouchers/my-vouchers
     */
    @GetMapping("/v1/vouchers/my-vouchers")
    public ResponseEntity<List<VoucherDTO>> getMyVouchers(
            @RequestHeader("X-User-Id") Long userId) {
        // Return available vouchers for now (can be enhanced later with user-specific logic)
        List<VoucherDTO> vouchers = voucherService.getValidVouchers();
        return ResponseEntity.ok(vouchers);
    }
    
    /**
     * Validate voucher by code (v1 API)
     * GET /api/v1/vouchers/validate/{code}
     */
    @GetMapping("/v1/vouchers/validate/{code}")
    public ResponseEntity<Map<String, Object>> validateVoucherByCode(@PathVariable String code) {
        try {
            VoucherDTO voucher = voucherService.getVoucherByCode(code);
            Map<String, Object> result = Map.of(
                "valid", true,
                "voucher", voucher,
                "message", "Voucher is valid"
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = Map.of(
                "valid", false,
                "message", "Voucher not found or invalid"
            );
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Validate voucher (Public - for checkout)
     * POST /api/vouchers/validate
     */
    @PostMapping("/vouchers/validate")
    public ResponseEntity<VoucherValidationResponse> validateVoucher(
            @Valid @RequestBody ValidateVoucherRequest request) {
        VoucherValidationResponse response = voucherService.validateVoucher(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get valid vouchers (Public - for customer to see available vouchers)
     * GET /api/vouchers/valid
     */
    @GetMapping("/vouchers/valid")
    public ResponseEntity<List<VoucherDTO>> getValidVouchers() {
        List<VoucherDTO> vouchers = voucherService.getValidVouchers();
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Get voucher by code (Public)
     * GET /api/vouchers/{code}
     */
    @GetMapping("/vouchers/{code}")
    public ResponseEntity<VoucherDTO> getVoucherByCode(@PathVariable String code) {
        VoucherDTO voucher = voucherService.getVoucherByCode(code);
        return ResponseEntity.ok(voucher);
    }

    /**
     * Search vouchers (Public)
     * GET /api/vouchers/search?q={pattern}
     */
    @GetMapping("/vouchers/search")
    public ResponseEntity<List<VoucherDTO>> searchVouchers(@RequestParam String q) {
        List<VoucherDTO> vouchers = voucherService.searchVouchers(q);
        return ResponseEntity.ok(vouchers);
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Get all vouchers (Admin)
     * GET /api/admin/vouchers
     */
    @GetMapping("/admin/vouchers")
    public ResponseEntity<List<VoucherDTO>> getAllVouchers(
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        
        List<VoucherDTO> vouchers;
        if (activeOnly) {
            vouchers = voucherService.getActiveVouchers();
        } else {
            vouchers = voucherService.getAllVouchers();
        }
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Get voucher by ID (Admin)
     * GET /api/admin/vouchers/{id}
     */
    @GetMapping("/admin/vouchers/{id}")
    public ResponseEntity<VoucherDTO> getVoucherById(@PathVariable Integer id) {
        VoucherDTO voucher = voucherService.getVoucherById(id);
        return ResponseEntity.ok(voucher);
    }

    /**
     * Create voucher (Admin)
     * POST /api/admin/vouchers
     */
    @PostMapping("/admin/vouchers")
    public ResponseEntity<VoucherDTO> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request) {
        VoucherDTO voucher = voucherService.createVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(voucher);
    }

    /**
     * Update voucher (Admin)
     * PUT /api/admin/vouchers/{id}
     */
    @PutMapping("/admin/vouchers/{id}")
    public ResponseEntity<VoucherDTO> updateVoucher(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateVoucherRequest request) {
        VoucherDTO voucher = voucherService.updateVoucher(id, request);
        return ResponseEntity.ok(voucher);
    }

    /**
     * Delete voucher (Admin)
     * DELETE /api/admin/vouchers/{id}
     */
    @DeleteMapping("/admin/vouchers/{id}")
    public ResponseEntity<Map<String, String>> deleteVoucher(@PathVariable Integer id) {
        voucherService.deleteVoucher(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Voucher deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate voucher (Admin - soft delete)
     * PATCH /api/admin/vouchers/{id}/deactivate
     */
    @PatchMapping("/admin/vouchers/{id}/deactivate")
    public ResponseEntity<VoucherDTO> deactivateVoucher(@PathVariable Integer id) {
        VoucherDTO voucher = voucherService.deactivateVoucher(id);
        return ResponseEntity.ok(voucher);
    }

    /**
     * Get vouchers expiring soon (Admin)
     * GET /api/admin/vouchers/expiring?days=7
     */
    @GetMapping("/admin/vouchers/expiring")
    public ResponseEntity<List<VoucherDTO>> getVouchersExpiringSoon(
            @RequestParam(defaultValue = "7") int days) {
        List<VoucherDTO> vouchers = voucherService.getVouchersExpiringSoon(days);
        return ResponseEntity.ok(vouchers);
    }

    /**
     * Get vouchers with low usage (Admin)
     * GET /api/admin/vouchers/low-usage?threshold=10
     */
    @GetMapping("/admin/vouchers/low-usage")
    public ResponseEntity<List<VoucherDTO>> getVouchersWithLowUsage(
            @RequestParam(defaultValue = "10") int threshold) {
        List<VoucherDTO> vouchers = voucherService.getVouchersWithLowUsage(threshold);
        return ResponseEntity.ok(vouchers);
    }
}
