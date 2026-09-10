package com.tiki.settlement.service;

import com.tiki.settlement.dto.CommissionRuleDto;
import com.tiki.settlement.entity.CommissionRuleEntity;
import com.tiki.settlement.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionRuleService {

    public static final BigDecimal DEFAULT_PLATFORM_COMMISSION_RATE = new BigDecimal("5.00"); // 5%

    private final CommissionRuleRepository commissionRuleRepository;

    public BigDecimal getApplicableRate(Long categoryId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Check if category-specific rule exists
        if (categoryId != null) {
            List<CommissionRuleEntity> categoryRules = commissionRuleRepository.findActiveByCategoryId(categoryId, now);
            if (!categoryRules.isEmpty()) {
                BigDecimal rate = categoryRules.get(0).getCommissionRate();
                log.info("Found category rule for category {}: {}%", categoryId, rate);
                return rate;
            }
        }

        // 2. Check global rule
        List<CommissionRuleEntity> globalRules = commissionRuleRepository.findActiveGlobalRules(now);
        if (!globalRules.isEmpty()) {
            BigDecimal rate = globalRules.get(0).getCommissionRate();
            log.info("Found global commission rule: {}%", rate);
            return rate;
        }

        // 3. Fallback default
        log.info("No active rule found, using default platform rate: {}%", DEFAULT_PLATFORM_COMMISSION_RATE);
        return DEFAULT_PLATFORM_COMMISSION_RATE;
    }

    @Transactional
    public CommissionRuleDto createRule(CommissionRuleDto dto) {
        CommissionRuleEntity entity = CommissionRuleEntity.builder()
                .categoryId(dto.getCategoryId())
                .commissionRate(dto.getCommissionRate())
                .effectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : LocalDateTime.now())
                .effectiveTo(dto.getEffectiveTo())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdBy(dto.getCreatedBy())
                .build();

        CommissionRuleEntity saved = commissionRuleRepository.save(entity);
        log.info("Created commission rule id={}, categoryId={}, rate={}%", saved.getId(), saved.getCategoryId(), saved.getCommissionRate());
        return toDto(saved);
    }

    public List<CommissionRuleDto> getAllRules() {
        return commissionRuleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        CommissionRuleEntity rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc hoa hồng id=" + ruleId));
        rule.setIsActive(false);
        commissionRuleRepository.save(rule);
        log.info("Deactivated commission rule id={}", ruleId);
    }

    private CommissionRuleDto toDto(CommissionRuleEntity entity) {
        return CommissionRuleDto.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .commissionRate(entity.getCommissionRate())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
