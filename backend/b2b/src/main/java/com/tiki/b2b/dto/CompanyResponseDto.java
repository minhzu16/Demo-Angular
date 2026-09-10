package com.tiki.b2b.dto;

import com.tiki.b2b.entity.CompanyEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDto {
    private Long id;
    private String companyName;
    private String taxId;
    private String businessLicense;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private CompanyEntity.CompanyStatus status;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private BigDecimal creditLimit;
    private Integer paymentTermDays;
    private LocalDateTime createdAt;
}
