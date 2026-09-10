package com.tiki.auth.service;

import com.tiki.auth.dto.MembershipBenefitsDto;
import com.tiki.auth.dto.MembershipResponseDto;
import com.tiki.auth.dto.MembershipSubscribeRequest;
import com.tiki.auth.entity.MembershipEntity;
import com.tiki.auth.entity.MembershipPlanEntity;
import com.tiki.auth.repository.MembershipPlanRepository;
import com.tiki.auth.repository.MembershipRepository;
import com.tiki.auth.repository.MembershipTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipPlanRepository membershipPlanRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipTransactionRepository membershipTransactionRepository;

    @InjectMocks
    private MembershipService membershipService;

    private MembershipPlanEntity plan;

    @BeforeEach
    void setUp() {
        plan = MembershipPlanEntity.builder()
                .id(1L)
                .code("TIKI_PRO_MONTHLY")
                .name("Tiki PRO Tháng")
                .priceMonthly(new BigDecimal("49000.00"))
                .priceYearly(new BigDecimal("499000.00"))
                .maxFreeShipPerMonth(10)
                .pointsMultiplier(2.0)
                .trialDays(7)
                .isActive(true)
                .build();
    }

    @Test
    void testSubscribe_Trial() {
        MembershipSubscribeRequest req = MembershipSubscribeRequest.builder()
                .planCode("TIKI_PRO_MONTHLY")
                .startTrial(true)
                .build();

        when(membershipPlanRepository.findByCode("TIKI_PRO_MONTHLY")).thenReturn(Optional.of(plan));
        when(membershipRepository.findActiveMembership(eq(10L), any())).thenReturn(Optional.empty());
        when(membershipRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(MembershipEntity.class))).thenAnswer(inv -> {
            MembershipEntity m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        MembershipResponseDto res = membershipService.subscribe(10L, req);

        assertNotNull(res);
        assertEquals(MembershipEntity.MembershipStatus.TRIAL, res.getStatus());
        assertTrue(res.getEndDate().isAfter(LocalDateTime.now().plusDays(6)));
        verify(membershipTransactionRepository, never()).save(any());
    }

    @Test
    void testSubscribe_PaidMonthly() {
        MembershipSubscribeRequest req = MembershipSubscribeRequest.builder()
                .planCode("TIKI_PRO_MONTHLY")
                .billingCycle("MONTHLY")
                .paymentMethodId("MOMO-12345")
                .startTrial(false)
                .build();

        when(membershipPlanRepository.findByCode("TIKI_PRO_MONTHLY")).thenReturn(Optional.of(plan));
        when(membershipRepository.findActiveMembership(eq(10L), any())).thenReturn(Optional.empty());
        when(membershipRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any(MembershipEntity.class))).thenAnswer(inv -> {
            MembershipEntity m = inv.getArgument(0);
            m.setId(101L);
            return m;
        });

        MembershipResponseDto res = membershipService.subscribe(10L, req);

        assertNotNull(res);
        assertEquals(MembershipEntity.MembershipStatus.ACTIVE, res.getStatus());
        verify(membershipTransactionRepository).save(any());
    }

    @Test
    void testSubscribe_ThrowsIfAlreadyActive() {
        MembershipSubscribeRequest req = MembershipSubscribeRequest.builder()
                .planCode("TIKI_PRO_MONTHLY")
                .build();

        when(membershipPlanRepository.findByCode("TIKI_PRO_MONTHLY")).thenReturn(Optional.of(plan));
        when(membershipRepository.findActiveMembership(eq(10L), any()))
                .thenReturn(Optional.of(new MembershipEntity()));

        assertThrows(IllegalStateException.class, () -> membershipService.subscribe(10L, req));
    }

    @Test
    void testCancelSubscription() {
        MembershipEntity m = MembershipEntity.builder()
                .id(1L)
                .userId(10L)
                .planId(1L)
                .status(MembershipEntity.MembershipStatus.ACTIVE)
                .autoRenew(true)
                .build();

        when(membershipRepository.findActiveMembership(eq(10L), any())).thenReturn(Optional.of(m));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        MembershipResponseDto res = membershipService.cancelSubscription(10L);

        assertFalse(res.getAutoRenew());
        assertNotNull(m.getCancelledAt());
    }

    @Test
    void testRenewSubscription() {
        LocalDateTime now = LocalDateTime.now();
        MembershipEntity m = MembershipEntity.builder()
                .id(1L)
                .userId(10L)
                .planId(1L)
                .status(MembershipEntity.MembershipStatus.ACTIVE)
                .endDate(now)
                .build();

        when(membershipRepository.findById(1L)).thenReturn(Optional.of(m));
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MembershipResponseDto res = membershipService.renewSubscription(1L);

        assertTrue(res.getEndDate().isAfter(now));
        verify(membershipTransactionRepository).save(any());
    }

    @Test
    void testGetBenefits_ActiveMember() {
        MembershipEntity m = MembershipEntity.builder()
                .id(1L)
                .userId(10L)
                .planId(1L)
                .freeShipUsedThisMonth(3)
                .status(MembershipEntity.MembershipStatus.ACTIVE)
                .build();

        when(membershipRepository.findActiveMembership(eq(10L), any())).thenReturn(Optional.of(m));
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        MembershipBenefitsDto benefits = membershipService.getBenefits(10L);

        assertTrue(benefits.isMember());
        assertTrue(benefits.isFreeShipping());
        assertEquals(7, benefits.getFreeShipRemaining()); // 10 - 3 = 7
        assertEquals(2.0, benefits.getPointsMultiplier());
    }

    @Test
    void testUseFreeShip() {
        MembershipEntity m = MembershipEntity.builder()
                .id(1L)
                .userId(10L)
                .planId(1L)
                .freeShipUsedThisMonth(9)
                .status(MembershipEntity.MembershipStatus.ACTIVE)
                .build();

        when(membershipRepository.findActiveMembership(eq(10L), any())).thenReturn(Optional.of(m));
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        // Use 10th free ship -> returns true
        boolean used = membershipService.useFreeShip(10L);
        assertTrue(used);
        assertEquals(10, m.getFreeShipUsedThisMonth());

        // Try 11th free ship -> exceeds quota, returns false
        boolean usedAgain = membershipService.useFreeShip(10L);
        assertFalse(usedAgain);
    }
}
