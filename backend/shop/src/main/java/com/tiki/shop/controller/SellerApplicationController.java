package com.tiki.shop.controller;

import com.tiki.common.entity.SellerApplication;
import com.tiki.shop.service.SellerApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller-applications")
@RequiredArgsConstructor
@Slf4j
public class SellerApplicationController {

    private final SellerApplicationService service;

    @PostMapping
    public ResponseEntity<SellerApplication> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody SellerApplication application) {
        log.info("Creating seller application for user: {}", userId);
        if (userId == null) throw new RuntimeException("Unauthorized");
        return ResponseEntity.ok(service.createApplication(userId, application));
    }

    @GetMapping("/my-application")
    public ResponseEntity<List<SellerApplication>> getMyApplications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) throw new RuntimeException("Unauthorized");
        return ResponseEntity.ok(service.getApplicationsByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<SellerApplication>> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.getAllByStatus(status));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<SellerApplication> review(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestBody Map<String, Object> payload) {
        boolean approved = (boolean) payload.get("approved");
        String reason = (String) payload.get("rejectionReason");
        if (approved) {
            return ResponseEntity.ok(service.approveApplication(id, adminId));
        } else {
            return ResponseEntity.ok(service.rejectApplication(id, adminId, reason));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<SellerApplication> approve(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {
        return ResponseEntity.ok(service.approveApplication(id, adminId));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<SellerApplication> reject(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long adminId,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(service.rejectApplication(id, adminId, payload.get("reason")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Exception in SellerApplicationController", e);
        e.printStackTrace();
        return ResponseEntity.status(500).body(e.getClass().getName() + ": " + e.getMessage());
    }
}
