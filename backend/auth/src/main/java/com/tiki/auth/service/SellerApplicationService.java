package com.tiki.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.auth.dto.ReviewSellerApplicationRequest;
import com.tiki.auth.dto.SellerApplicationRequest;
import com.tiki.auth.dto.SellerApplicationResponse;
import com.tiki.auth.entity.User;
import com.tiki.auth.exception.BadRequestException;
import com.tiki.auth.exception.UserNotFoundException;
import com.tiki.auth.repository.UserRepository;
import com.tiki.auth.repository.SellerApplicationRepository;
import com.tiki.common.entity.SellerApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerApplicationService {
    
    private final SellerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    /**
     * Buyer submits seller application
     */
    @Transactional
    public SellerApplicationResponse submitApplication(SellerApplicationRequest request) {
        User user = getCurrentUser();
        
        // Check if user is already a seller
        if (user.getRole() == User.Role.SELLER) {
            throw new BadRequestException("You are already a seller");
        }
        
        // Check if there's already a pending application
        if (applicationRepository.existsByUserIdAndStatus(user.getId(), SellerApplication.Status.PENDING)) {
            throw new BadRequestException("You already have a pending application");
        }
        
        SellerApplication application = new SellerApplication();
        application.setUserId(user.getId());
        application.setShopName(request.getShopName());
        application.setShopDescription(request.getShopDescription());
        application.setBusinessLicense(request.getBusinessLicense());
        application.setTaxCode(request.getTaxCode());
        application.setPhoneNumber(request.getPhoneNumber());
        application.setAddress(request.getAddress());
        
        // Convert category IDs to JSON
        try {
            application.setCategories(objectMapper.writeValueAsString(request.getCategoryIds()));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid category data");
        }
        
        application.setStatus(SellerApplication.Status.PENDING);
        
        SellerApplication saved = applicationRepository.save(application);
        return toResponse(saved);
    }
    
    /**
     * Get current user's application status
     */
    public SellerApplicationResponse getMyApplication() {
        User user = getCurrentUser();
        
        SellerApplication application = applicationRepository.findByUserIdAndStatus(
                user.getId(), SellerApplication.Status.PENDING)
                .orElse(null);
        
        if (application == null) {
            // Check for latest application (approved or rejected)
            List<SellerApplication> apps = applicationRepository.findByUserId(user.getId());
            if (apps.isEmpty()) {
                return null;
            }
            application = apps.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .findFirst()
                    .orElse(null);
        }
        
        return application != null ? toResponse(application) : null;
    }
    
    /**
     * Admin gets all pending applications
     */
    public List<SellerApplicationResponse> getPendingApplications() {
        return applicationRepository.findByStatus(SellerApplication.Status.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Admin gets all applications
     */
    public List<SellerApplicationResponse> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Admin reviews application (approve or reject)
     */
    @Transactional
    public SellerApplicationResponse reviewApplication(Long applicationId, ReviewSellerApplicationRequest request) {
        User admin = getCurrentUser();
        
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Only admin can review applications");
        }
        
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Application not found"));
        
        if (application.getStatus() != SellerApplication.Status.PENDING) {
            throw new BadRequestException("Application already reviewed");
        }
        
        if (request.getApproved()) {
            // Approve: Update application status and user role
            application.setStatus(SellerApplication.Status.APPROVED);
            
            User user = userRepository.findById(application.getUserId())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            user.setRole(User.Role.SELLER);
            userRepository.save(user);
            
        } else {
            // Reject: Update status and add reason
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Rejection reason is required");
            }
            application.setStatus(SellerApplication.Status.REJECTED);
            application.setRejectionReason(request.getRejectionReason());
        }
        
        application.setReviewedBy(admin.getId());
        application.setReviewedAt(LocalDateTime.now());
        
        SellerApplication saved = applicationRepository.save(application);
        
        // TODO: Send notification to user
        
        return toResponse(saved);
    }
    
    private SellerApplicationResponse toResponse(SellerApplication entity) {
        SellerApplicationResponse response = new SellerApplicationResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setShopName(entity.getShopName());
        response.setShopDescription(entity.getShopDescription());
        response.setBusinessLicense(entity.getBusinessLicense());
        response.setTaxCode(entity.getTaxCode());
        response.setPhoneNumber(entity.getPhoneNumber());
        response.setAddress(entity.getAddress());
        
        // Parse categories JSON
        try {
            if (entity.getCategories() != null) {
                List<Integer> categoryIds = objectMapper.readValue(
                        entity.getCategories(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class)
                );
                response.setCategoryIds(categoryIds);
            }
        } catch (JsonProcessingException e) {
            // Ignore parsing error
        }
        
        response.setStatus(entity.getStatus().name());
        response.setRejectionReason(entity.getRejectionReason());
        response.setReviewedBy(entity.getReviewedBy());
        response.setReviewedAt(entity.getReviewedAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        return response;
    }
}
