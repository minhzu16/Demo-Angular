package com.tiki.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewSellerApplicationRequest {
    
    @NotNull(message = "Approved status is required")
    private Boolean approved; // true = approve, false = reject
    
    private String rejectionReason; // Required if approved = false
}
