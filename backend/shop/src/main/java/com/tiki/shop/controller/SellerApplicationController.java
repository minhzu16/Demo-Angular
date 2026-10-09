package com.tiki.shop.controller;

import com.tiki.common.entity.SellerApplication;
import com.tiki.shop.service.SellerApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Reviewing applications grants the SELLER role, so list/review/approve/reject are ADMIN-only. They used to be
 * reachable by anyone (the gateway only guards paths containing "/admin/"), i.e. anyone could approve their own
 * application. The gateway enforces the same rule; this is the in-service second line.
 */
@RestController
@RequestMapping("/api/v1/seller-applications")
@RequiredArgsConstructor
@Slf4j
public class SellerApplicationController {

    private final SellerApplicationService service;

    private static void requireAdmin(Long adminId, String role) {
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        if (role == null || !role.toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới duyệt hồ sơ người bán");
        }
    }

    private static Long requireUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return userId;
    }

    @PostMapping
    public ResponseEntity<SellerApplication> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody SellerApplication application) {
        log.info("Creating seller application for user: {}", userId);
        try {
            return ResponseEntity.ok(service.createApplication(requireUser(userId), application));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/my-application")
    public ResponseEntity<List<SellerApplication>> getMyApplications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(service.getApplicationsByUserId(requireUser(userId)));
    }

    @GetMapping
    public ResponseEntity<List<SellerApplication>> getAll(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(adminId, role);
        return ResponseEntity.ok(service.getAllByStatus(status));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<SellerApplication> review(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody Map<String, Object> payload) {
        requireAdmin(adminId, role);
        boolean approved = Boolean.TRUE.equals(payload.get("approved"));
        if (approved) {
            return ResponseEntity.ok(service.approveApplication(id, adminId));
        }
        return ResponseEntity.ok(service.rejectApplication(id, adminId, (String) payload.get("rejectionReason")));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<SellerApplication> approve(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(adminId, role);
        return ResponseEntity.ok(service.approveApplication(id, adminId));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<SellerApplication> reject(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody Map<String, String> payload) {
        requireAdmin(adminId, role);
        return ResponseEntity.ok(service.rejectApplication(id, adminId, payload.get("reason")));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason())));
    }

    /** Business-rule failures (already processed, not found, ...) — no class names or stack traces to the client. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException e) {
        log.error("Exception in SellerApplicationController", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", String.valueOf(e.getMessage())));
    }
}
