package com.tiki.auth.service;

import com.tiki.auth.dto.*;
import com.tiki.auth.entity.MembershipEntity;
import com.tiki.auth.entity.MembershipPlanEntity;
import com.tiki.auth.entity.MembershipTransactionEntity;
import com.tiki.auth.repository.MembershipPlanRepository;
import com.tiki.auth.repository.MembershipRepository;
import com.tiki.auth.repository.MembershipTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipTransactionRepository membershipTransactionRepository;

    @Transactional
    public MembershipResponseDto subscribe(Long userId, MembershipSubscribeRequest req) {
        MembershipPlanEntity plan = membershipPlanRepository.findByCode(req.getPlanCode())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói hội viên: " + req.getPlanCode()));

        if (!plan.getIsActive()) {
            throw new IllegalStateException("Gói hội viên hiện tại đã ngừng kích hoạt.");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<MembershipEntity> existing = membershipRepository.findActiveMembership(userId, now);
        if (existing.isPresent()) {
            throw new IllegalStateException("Bạn đã có gói hội viên đang hoạt động.");
        }

        boolean isTrial = Boolean.TRUE.equals(req.getStartTrial()) && plan.getTrialDays() != null && plan.getTrialDays() > 0;
        // One free trial per user: re-subscribing with startTrial after each expiry gave unlimited free PRO.
        if (isTrial && membershipRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Bạn đã sử dụng dùng thử miễn phí trước đó.");
        }
        LocalDateTime startDate = now;
        LocalDateTime endDate;
        MembershipEntity.MembershipStatus status;
        BigDecimal chargeAmount;

        if (isTrial) {
            status = MembershipEntity.MembershipStatus.TRIAL;
            endDate = now.plusDays(plan.getTrialDays());
            chargeAmount = BigDecimal.ZERO;
        } else {
            status = MembershipEntity.MembershipStatus.ACTIVE;
            if ("YEARLY".equalsIgnoreCase(req.getBillingCycle())) {
                endDate = now.plusYears(1);
                chargeAmount = plan.getPriceYearly() != null ? plan.getPriceYearly() : plan.getPriceMonthly().multiply(BigDecimal.valueOf(12));
            } else {
                endDate = now.plusMonths(1);
                chargeAmount = plan.getPriceMonthly();
            }
        }

        MembershipEntity membership = membershipRepository.findByUserId(userId).orElse(new MembershipEntity());
        membership.setUserId(userId);
        membership.setPlanId(plan.getId());
        membership.setStatus(status);
        membership.setStartDate(startDate);
        membership.setEndDate(endDate);
        membership.setNextBillingDate(endDate);
        membership.setAutoRenew(true);
        membership.setPaymentMethodId(req.getPaymentMethodId());
        membership.setFreeShipUsedThisMonth(0);

        MembershipEntity saved = membershipRepository.save(membership);

        // Record transaction
        if (chargeAmount.compareTo(BigDecimal.ZERO) > 0) {
            MembershipTransactionEntity tx = MembershipTransactionEntity.builder()
                    .membershipId(saved.getId())
                    .type(MembershipTransactionEntity.TransactionType.SUBSCRIPTION)
                    .amount(chargeAmount)
                    .paymentRef(req.getPaymentMethodId())
                    .build();
            membershipTransactionRepository.save(tx);
        }

        log.info("User {} subscribed to plan {} (status={}, endDate={})", userId, plan.getCode(), status, endDate);
        return toDto(saved, plan);
    }

    @Transactional
    public MembershipResponseDto cancelSubscription(Long userId) {
        MembershipEntity membership = membershipRepository.findActiveMembership(userId, LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("Bạn không có gói hội viên nào đang hoạt động."));

        membership.setAutoRenew(false);
        membership.setCancelledAt(LocalDateTime.now());
        MembershipEntity saved = membershipRepository.save(membership);

        MembershipPlanEntity plan = membershipPlanRepository.findById(saved.getPlanId()).orElse(null);
        log.info("User {} cancelled auto-renew for membership id={}", userId, saved.getId());
        return toDto(saved, plan);
    }

    @Transactional
    public MembershipResponseDto renewSubscription(Long membershipId) {
        MembershipEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội viên id=" + membershipId));

        MembershipPlanEntity plan = membershipPlanRepository.findById(membership.getPlanId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy gói hội viên của id=" + membershipId));

        LocalDateTime newEndDate = membership.getEndDate().plusMonths(1);
        membership.setStatus(MembershipEntity.MembershipStatus.ACTIVE);
        membership.setEndDate(newEndDate);
        membership.setNextBillingDate(newEndDate);
        membership.setFreeShipUsedThisMonth(0); // Reset monthly freeship quota
        MembershipEntity saved = membershipRepository.save(membership);

        MembershipTransactionEntity tx = MembershipTransactionEntity.builder()
                .membershipId(saved.getId())
                .type(MembershipTransactionEntity.TransactionType.RENEWAL)
                .amount(plan.getPriceMonthly())
                .paymentRef(membership.getPaymentMethodId())
                .build();
        membershipTransactionRepository.save(tx);

        log.info("Renewed membership id={}, new endDate={}", membershipId, newEndDate);
        return toDto(saved, plan);
    }

    public boolean isMember(Long userId) {
        return membershipRepository.findActiveMembership(userId, LocalDateTime.now()).isPresent();
    }

    public MembershipBenefitsDto getBenefits(Long userId) {
        Optional<MembershipEntity> optMembership = membershipRepository.findActiveMembership(userId, LocalDateTime.now());
        if (optMembership.isEmpty()) {
            return MembershipBenefitsDto.builder()
                    .isMember(false)
                    .pointsMultiplier(1.0)
                    .freeShipping(false)
                    .freeShipRemaining(0)
                    .exclusiveDeals(false)
                    .prioritySupport(false)
                    .build();
        }

        MembershipEntity membership = optMembership.get();
        MembershipPlanEntity plan = membershipPlanRepository.findById(membership.getPlanId()).orElse(null);

        int maxFreeShip = plan != null && plan.getMaxFreeShipPerMonth() != null ? plan.getMaxFreeShipPerMonth() : 10;
        int used = membership.getFreeShipUsedThisMonth() != null ? membership.getFreeShipUsedThisMonth() : 0;
        int remaining = Math.max(0, maxFreeShip - used);
        double multiplier = plan != null && plan.getPointsMultiplier() != null ? plan.getPointsMultiplier() : 2.0;
        BigDecimal maxValue = plan != null ? plan.getFreeShipMaxValue() : null;

        return MembershipBenefitsDto.builder()
                .isMember(true)
                .planName(plan != null ? plan.getName() : "VIP")
                .freeShipping(true)
                .freeShipRemaining(remaining)
                .freeShipMaxValue(maxValue)
                .pointsMultiplier(multiplier)
                .exclusiveDeals(true)
                .prioritySupport(true)
                .build();
    }

    @Transactional
    public boolean useFreeShip(Long userId) {
        Optional<MembershipEntity> optMembership = membershipRepository.findActiveMembership(userId, LocalDateTime.now());
        if (optMembership.isEmpty()) {
            return false;
        }

        MembershipEntity membership = optMembership.get();
        MembershipPlanEntity plan = membershipPlanRepository.findById(membership.getPlanId()).orElse(null);
        int max = plan != null && plan.getMaxFreeShipPerMonth() != null ? plan.getMaxFreeShipPerMonth() : 10;
        int used = membership.getFreeShipUsedThisMonth() != null ? membership.getFreeShipUsedThisMonth() : 0;

        if (used < max) {
            membership.setFreeShipUsedThisMonth(used + 1);
            membershipRepository.save(membership);
            log.info("User {} used freeship benefit ({}/{})", userId, used + 1, max);
            return true;
        }
        return false;
    }

    public MembershipResponseDto getMyMembership(Long userId) {
        Optional<MembershipEntity> optMembership = membershipRepository.findActiveMembership(userId, LocalDateTime.now());
        if (optMembership.isEmpty()) {
            return MembershipResponseDto.builder()
                    .userId(userId)
                    .isMember(false)
                    .build();
        }
        MembershipEntity membership = optMembership.get();
        MembershipPlanEntity plan = membershipPlanRepository.findById(membership.getPlanId()).orElse(null);
        return toDto(membership, plan);
    }

    public List<MembershipPlanDto> getAllPlans() {
        return membershipPlanRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toPlanDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MembershipPlanDto createPlan(MembershipPlanDto dto) {
        MembershipPlanEntity entity = MembershipPlanEntity.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .priceMonthly(dto.getPriceMonthly())
                .priceYearly(dto.getPriceYearly())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "VND")
                .benefits(dto.getBenefits())
                .maxFreeShipPerMonth(dto.getMaxFreeShipPerMonth() != null ? dto.getMaxFreeShipPerMonth() : 10)
                .freeShipMaxValue(dto.getFreeShipMaxValue())
                .pointsMultiplier(dto.getPointsMultiplier() != null ? dto.getPointsMultiplier() : 2.0)
                .trialDays(dto.getTrialDays() != null ? dto.getTrialDays() : 7)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();

        MembershipPlanEntity saved = membershipPlanRepository.save(entity);
        log.info("Created membership plan: {}", saved.getCode());
        return toPlanDto(saved);
    }

    private MembershipResponseDto toDto(MembershipEntity m, MembershipPlanEntity p) {
        return MembershipResponseDto.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .planId(m.getPlanId())
                .planCode(p != null ? p.getCode() : null)
                .planName(p != null ? p.getName() : null)
                .status(m.getStatus())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .nextBillingDate(m.getNextBillingDate())
                .autoRenew(m.getAutoRenew())
                .freeShipUsedThisMonth(m.getFreeShipUsedThisMonth())
                .maxFreeShipPerMonth(p != null ? p.getMaxFreeShipPerMonth() : 10)
                .pointsMultiplier(p != null ? p.getPointsMultiplier() : 2.0)
                .isMember(true)
                .build();
    }

    private MembershipPlanDto toPlanDto(MembershipPlanEntity p) {
        return MembershipPlanDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .priceMonthly(p.getPriceMonthly())
                .priceYearly(p.getPriceYearly())
                .currency(p.getCurrency())
                .benefits(p.getBenefits())
                .maxFreeShipPerMonth(p.getMaxFreeShipPerMonth())
                .freeShipMaxValue(p.getFreeShipMaxValue())
                .pointsMultiplier(p.getPointsMultiplier())
                .trialDays(p.getTrialDays())
                .isActive(p.getIsActive())
                .displayOrder(p.getDisplayOrder())
                .build();
    }
}
