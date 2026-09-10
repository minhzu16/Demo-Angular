package com.tiki.settlement.service;

import com.tiki.settlement.dto.CommissionRuleDto;
import com.tiki.settlement.entity.CommissionRuleEntity;
import com.tiki.settlement.repository.CommissionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionRuleServiceTest {

    @Mock
    private CommissionRuleRepository commissionRuleRepository;

    @InjectMocks
    private CommissionRuleService commissionRuleService;

    private CommissionRuleEntity categoryRule;
    private CommissionRuleEntity globalRule;

    @BeforeEach
    void setUp() {
        categoryRule = CommissionRuleEntity.builder()
                .id(1L)
                .categoryId(10L)
                .commissionRate(new BigDecimal("8.00"))
                .isActive(true)
                .build();

        globalRule = CommissionRuleEntity.builder()
                .id(2L)
                .categoryId(null)
                .commissionRate(new BigDecimal("4.50"))
                .isActive(true)
                .build();
    }

    @Test
    void testGetApplicableRate_CategoryRuleFound() {
        when(commissionRuleRepository.findActiveByCategoryId(eq(10L), any(LocalDateTime.class)))
                .thenReturn(List.of(categoryRule));

        BigDecimal rate = commissionRuleService.getApplicableRate(10L);
        assertEquals(new BigDecimal("8.00"), rate);
    }

    @Test
    void testGetApplicableRate_FallbackToGlobalRule() {
        when(commissionRuleRepository.findActiveByCategoryId(eq(99L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(List.of(globalRule));

        BigDecimal rate = commissionRuleService.getApplicableRate(99L);
        assertEquals(new BigDecimal("4.50"), rate);
    }

    @Test
    void testGetApplicableRate_FallbackToDefaultPlatformRate() {
        when(commissionRuleRepository.findActiveGlobalRules(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        BigDecimal rate = commissionRuleService.getApplicableRate(null);
        assertEquals(CommissionRuleService.DEFAULT_PLATFORM_COMMISSION_RATE, rate);
    }

    @Test
    void testCreateRule() {
        CommissionRuleDto dto = CommissionRuleDto.builder()
                .categoryId(5L)
                .commissionRate(new BigDecimal("6.50"))
                .createdBy(1L)
                .build();

        when(commissionRuleRepository.save(any(CommissionRuleEntity.class)))
                .thenAnswer(inv -> {
                    CommissionRuleEntity e = inv.getArgument(0);
                    e.setId(100L);
                    return e;
                });

        CommissionRuleDto result = commissionRuleService.createRule(dto);
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(new BigDecimal("6.50"), result.getCommissionRate());
    }

    @Test
    void testDeleteRule() {
        when(commissionRuleRepository.findById(1L)).thenReturn(Optional.of(categoryRule));

        commissionRuleService.deleteRule(1L);

        assertFalse(categoryRule.getIsActive());
        verify(commissionRuleRepository).save(categoryRule);
    }
}
