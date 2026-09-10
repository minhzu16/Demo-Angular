package com.tiki.order.controller;

import com.tiki.common.dto.ComplaintDto;
import com.tiki.common.dto.CreateComplaintRequest;
import com.tiki.common.dto.ResolveComplaintRequest;
import com.tiki.order.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * Buyer submits a complaint for an order
     */
    @PostMapping
    public ResponseEntity<ComplaintDto> createComplaint(
            @RequestHeader("X-User-Id") Long buyerId,
            @Valid @RequestBody CreateComplaintRequest request) {
        log.info("Received complaint submission from buyerId={} for orderId={}", buyerId, request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintService.createComplaint(buyerId, request));
    }

    /**
     * Buyer views all their submitted complaints
     */
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintDto>> getMyComplaints(
            @RequestHeader("X-User-Id") Long buyerId) {
        return ResponseEntity.ok(complaintService.getMyComplaints(buyerId));
    }

    /**
     * Seller views complaints regarding orders for their shop
     */
    @GetMapping("/seller")
    public ResponseEntity<List<ComplaintDto>> getSellerComplaints(
            @RequestHeader("X-User-Id") Long sellerId) {
        return ResponseEntity.ok(complaintService.getSellerComplaints(sellerId));
    }

    /**
     * View complaints for a specific order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ComplaintDto>> getComplaintsByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader(value = "X-User-Role", defaultValue = "BUYER") String role) {
        boolean isAdmin = role != null && role.toUpperCase().contains("ADMIN");
        return ResponseEntity.ok(complaintService.getComplaintsByOrderId(orderId, requesterId, isAdmin));
    }

    /**
     * Seller or Admin resolves or rejects a complaint
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<ComplaintDto> resolveComplaint(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long resolverId,
            @Valid @RequestBody ResolveComplaintRequest request) {
        log.info("Received complaint resolution request for id={} by resolverId={}", id, resolverId);
        return ResponseEntity.ok(complaintService.resolveComplaint(id, resolverId, request));
    }
}
