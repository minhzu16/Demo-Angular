package com.tiki.b2b.service;

import com.tiki.b2b.dto.B2BPriceTierDto;
import com.tiki.b2b.dto.CompanyRegistrationDto;
import com.tiki.b2b.dto.CompanyResponseDto;
import com.tiki.b2b.entity.B2BPriceTierEntity;
import com.tiki.b2b.entity.CompanyEntity;
import com.tiki.b2b.entity.CompanyUserEntity;
import com.tiki.b2b.repository.B2BPriceTierRepository;
import com.tiki.b2b.repository.CompanyRepository;
import com.tiki.b2b.repository.CompanyUserRepository;
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
public class B2BService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final B2BPriceTierRepository b2bPriceTierRepository;

    @Transactional
    public CompanyResponseDto registerCompany(CompanyRegistrationDto dto, Long userId) {
        if (companyRepository.findByTaxId(dto.getTaxId()).isPresent()) {
            throw new IllegalArgumentException("Mã số thuế " + dto.getTaxId() + " đã được đăng ký trước đó.");
        }

        CompanyEntity company = CompanyEntity.builder()
                .companyName(dto.getCompanyName())
                .taxId(dto.getTaxId())
                .businessLicense(dto.getBusinessLicense())
                .contactPerson(dto.getContactPerson())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .address(dto.getAddress())
                .status(CompanyEntity.CompanyStatus.PENDING_VERIFICATION)
                .creditLimit(BigDecimal.ZERO)
                .paymentTermDays(30)
                .build();

        CompanyEntity savedCompany = companyRepository.save(company);

        // Bind creator as Company ADMIN
        CompanyUserEntity adminUser = CompanyUserEntity.builder()
                .companyId(savedCompany.getId())
                .userId(userId)
                .role(CompanyUserEntity.CompanyRole.ADMIN)
                .isActive(true)
                .build();
        companyUserRepository.save(adminUser);

        log.info("Company {} registered by userId={}, pending verification", savedCompany.getCompanyName(), userId);
        return toCompanyDto(savedCompany);
    }

    @Transactional
    public CompanyResponseDto verifyCompany(Long companyId, Long adminId, BigDecimal creditLimit, Integer paymentTerms) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy doanh nghiệp id=" + companyId));

        company.setStatus(CompanyEntity.CompanyStatus.ACTIVE);
        company.setVerifiedBy(adminId);
        company.setVerifiedAt(LocalDateTime.now());
        if (creditLimit != null) company.setCreditLimit(creditLimit);
        if (paymentTerms != null) company.setPaymentTermDays(paymentTerms);

        CompanyEntity saved = companyRepository.save(company);
        log.info("Company {} verified by adminId={}, creditLimit={}", saved.getCompanyName(), adminId, creditLimit);
        return toCompanyDto(saved);
    }

    public BigDecimal getB2BPrice(Long productId, Integer quantity, BigDecimal defaultPrice) {
        if (quantity == null || quantity <= 0) {
            return defaultPrice;
        }

        List<B2BPriceTierEntity> tiers = b2bPriceTierRepository.findByProductIdAndIsActiveTrueOrderByMinQuantityAsc(productId);
        for (B2BPriceTierEntity tier : tiers) {
            boolean minMatch = quantity >= tier.getMinQuantity();
            boolean maxMatch = tier.getMaxQuantity() == null || quantity <= tier.getMaxQuantity();
            if (minMatch && maxMatch) {
                return tier.getUnitPrice();
            }
        }

        return defaultPrice;
    }

    @Transactional
    public B2BPriceTierDto addPriceTier(B2BPriceTierDto dto) {
        B2BPriceTierEntity entity = B2BPriceTierEntity.builder()
                .productId(dto.getProductId())
                .minQuantity(dto.getMinQuantity())
                .maxQuantity(dto.getMaxQuantity())
                .unitPrice(dto.getUnitPrice())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        B2BPriceTierEntity saved = b2bPriceTierRepository.save(entity);
        log.info("Created B2B price tier for product {}, minQty={}, price={}",
                saved.getProductId(), saved.getMinQuantity(), saved.getUnitPrice());
        return toTierDto(saved);
    }

    public List<B2BPriceTierDto> getPriceTiers(Long productId) {
        return b2bPriceTierRepository.findByProductIdAndIsActiveTrueOrderByMinQuantityAsc(productId).stream()
                .map(this::toTierDto)
                .collect(Collectors.toList());
    }

    public CompanyResponseDto getCompany(Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy doanh nghiệp id=" + companyId));
        return toCompanyDto(company);
    }

    private CompanyResponseDto toCompanyDto(CompanyEntity e) {
        return CompanyResponseDto.builder()
                .id(e.getId())
                .companyName(e.getCompanyName())
                .taxId(e.getTaxId())
                .businessLicense(e.getBusinessLicense())
                .contactPerson(e.getContactPerson())
                .contactEmail(e.getContactEmail())
                .contactPhone(e.getContactPhone())
                .address(e.getAddress())
                .status(e.getStatus())
                .verifiedBy(e.getVerifiedBy())
                .verifiedAt(e.getVerifiedAt())
                .creditLimit(e.getCreditLimit())
                .paymentTermDays(e.getPaymentTermDays())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private B2BPriceTierDto toTierDto(B2BPriceTierEntity e) {
        return B2BPriceTierDto.builder()
                .id(e.getId())
                .productId(e.getProductId())
                .minQuantity(e.getMinQuantity())
                .maxQuantity(e.getMaxQuantity())
                .unitPrice(e.getUnitPrice())
                .isActive(e.getIsActive())
                .build();
    }
}
