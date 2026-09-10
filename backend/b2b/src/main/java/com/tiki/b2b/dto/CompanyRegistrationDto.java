package com.tiki.b2b.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRegistrationDto {

    @NotBlank(message = "Tên doanh nghiệp không được để trống")
    private String companyName;

    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxId;

    private String businessLicense;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String address;
}
