package com.tiki.auth.controller;

import com.tiki.auth.dto.*;
import com.tiki.auth.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlanDto>> getPlans() {
        return ResponseEntity.ok(membershipService.getAllPlans());
    }

    @PostMapping("/plans")
    public ResponseEntity<MembershipPlanDto> createPlan(@Valid @RequestBody MembershipPlanDto dto) {
        return ResponseEntity.ok(membershipService.createPlan(dto));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<MembershipResponseDto> subscribe(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody MembershipSubscribeRequest req) {
        return ResponseEntity.ok(membershipService.subscribe(userId, req));
    }

    @GetMapping("/my")
    public ResponseEntity<MembershipResponseDto> getMyMembership(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(membershipService.getMyMembership(userId));
    }

    @PostMapping("/cancel")
    public ResponseEntity<MembershipResponseDto> cancelSubscription(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(membershipService.cancelSubscription(userId));
    }

    @GetMapping("/benefits")
    public ResponseEntity<MembershipBenefitsDto> getBenefits(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(membershipService.getBenefits(userId));
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<MembershipBenefitsDto> checkMemberBenefits(@PathVariable Long userId) {
        return ResponseEntity.ok(membershipService.getBenefits(userId));
    }

    @PostMapping("/use-freeship/{userId}")
    public ResponseEntity<Boolean> useFreeShip(@PathVariable Long userId) {
        return ResponseEntity.ok(membershipService.useFreeShip(userId));
    }
}
