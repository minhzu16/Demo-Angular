package com.tiki.b2b.service;

import com.tiki.b2b.dto.CompanyRegistrationDto;
import com.tiki.b2b.dto.CompanyResponseDto;
import com.tiki.b2b.entity.B2BPriceTierEntity;
import com.tiki.b2b.entity.CompanyEntity;
import com.tiki.b2b.repository.B2BPriceTierRepository;
import com.tiki.b2b.repository.CompanyRepository;
import com.tiki.b2b.repository.CompanyUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class B2BServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyUserRepository companyUserRepository;

    @Mock
    private B2BPriceTierRepository b2bPriceTierRepository;

    @InjectMocks
    private B2BService b2bService;

    private CompanyEntity pendingCompany;

    @BeforeEach
    void setUp() {
        pendingCompany = CompanyEntity.builder()
                .id(1L)
                .companyName("ABC Enterprise")
                .taxId("0102030405")
                .status(CompanyEntity.CompanyStatus.PENDING_VERIFICATION)
                .build();
    }

    @Test
    void testRegisterCompany_Success() {
        CompanyRegistrationDto dto = CompanyRegistrationDto.builder()
                .companyName("ABC Enterprise")
                .taxId("0102030405")
                .contactPerson("Nguyen Van B")
                .build();

        when(companyRepository.findByTaxId("0102030405")).thenReturn(Optional.empty());
        when(companyRepository.save(any(CompanyEntity.class))).thenAnswer(inv -> {
            CompanyEntity c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CompanyResponseDto res = b2bService.registerCompany(dto, 1L);

        assertNotNull(res);
        assertEquals(10L, res.getId());
        assertEquals(CompanyEntity.CompanyStatus.PENDING_VERIFICATION, res.getStatus());
        verify(companyUserRepository).save(any());
    }

    @Test
    void testRegisterCompany_DuplicateTaxId_Throws() {
        CompanyRegistrationDto dto = CompanyRegistrationDto.builder()
                .taxId("0102030405")
                .build();

        when(companyRepository.findByTaxId("0102030405")).thenReturn(Optional.of(pendingCompany));

        assertThrows(IllegalArgumentException.class, () -> b2bService.registerCompany(dto, 1L));
    }

    @Test
    void testVerifyCompany_Success() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(pendingCompany));
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompanyResponseDto res = b2bService.verifyCompany(1L, 999L, new BigDecimal("100000000.00"), 45);

        assertEquals(CompanyEntity.CompanyStatus.ACTIVE, res.getStatus());
        assertEquals(999L, res.getVerifiedBy());
        assertEquals(new BigDecimal("100000000.00"), res.getCreditLimit());
        assertEquals(45, res.getPaymentTermDays());
    }

    @Test
    void testGetB2BPrice_TierMatching() {
        // Tiers: 10-49: 90k, 50+: 80k
        B2BPriceTierEntity tier1 = B2BPriceTierEntity.builder()
                .minQuantity(10)
                .maxQuantity(49)
                .unitPrice(new BigDecimal("90000.00"))
                .build();

        B2BPriceTierEntity tier2 = B2BPriceTierEntity.builder()
                .minQuantity(50)
                .maxQuantity(null)
                .unitPrice(new BigDecimal("80000.00"))
                .build();

        when(b2bPriceTierRepository.findByProductIdAndIsActiveTrueOrderByMinQuantityAsc(101L))
                .thenReturn(List.of(tier1, tier2));

        BigDecimal defaultPrice = new BigDecimal("100000.00");

        // Buy 5 items -> below tier1 min, returns default 100k
        assertEquals(defaultPrice, b2bService.getB2BPrice(101L, 5, defaultPrice));

        // Buy 25 items -> matches tier1, returns 90k
        assertEquals(new BigDecimal("90000.00"), b2bService.getB2BPrice(101L, 25, defaultPrice));

        // Buy 100 items -> matches tier2, returns 80k
        assertEquals(new BigDecimal("80000.00"), b2bService.getB2BPrice(101L, 100, defaultPrice));
    }
}
