package com.tiki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SellerApplicationRequest {
    
    @NotBlank(message = "Shop name is required")
    @Size(min = 3, max = 150, message = "Shop name must be between 3 and 150 characters")
    private String shopName;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String shopDescription;
    
    private String businessLicense;
    
    private String taxCode;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotEmpty(message = "At least one category is required")
    private List<Integer> categoryIds; // Danh sách ngành hàng
}
