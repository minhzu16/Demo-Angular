package com.tiki.auth.controller;

import com.tiki.auth.dto.ReviewSellerApplicationRequest;
import com.tiki.auth.dto.SellerApplicationRequest;
import com.tiki.auth.dto.SellerApplicationResponse;
import com.tiki.auth.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller-applications")
public class SellerApplicationController {
    
    private final SellerApplicationService applicationService;
    
    public SellerApplicationController(SellerApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    
    /**
     * Buyer submits seller application
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SellerApplicationResponse> submitApplication(
            @Valid @RequestBody SellerApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.submitApplication(request));
    }
    
    /**
     * Get current user's application status
     */
    @GetMapping("/my-application")
    public ResponseEntity<SellerApplicationResponse> getMyApplication() {
        SellerApplicationResponse response = applicationService.getMyApplication();
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * Admin: Get all pending applications
     */
    @GetMapping("/pending")
    public ResponseEntity<List<SellerApplicationResponse>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getPendingApplications());
    }
    
    /**
     * Admin: Get all applications
     */
    @GetMapping
    public ResponseEntity<List<SellerApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }
    
    /**
     * Admin: Review application (approve or reject)
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<SellerApplicationResponse> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody ReviewSellerApplicationRequest request) {
        return ResponseEntity.ok(applicationService.reviewApplication(id, request));
    }
}
