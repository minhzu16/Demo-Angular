package com.tiki.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SellerApplicationResponse {
    
    private Long id;
    private Long userId;
    private String shopName;
    private String shopDescription;
    private String businessLicense;
    private String taxCode;
    private String phoneNumber;
    private String address;
    private List<Integer> categoryIds;
    private String status;
    private String rejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
