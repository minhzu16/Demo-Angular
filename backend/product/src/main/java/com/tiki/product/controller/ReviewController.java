package com.tiki.product.controller;

import com.tiki.product.dto.ReviewDto;
import com.tiki.product.dto.ReviewSummaryDto;
import com.tiki.product.service.ReviewService;
import com.tiki.product.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;
    
    @PostMapping({"/reviews/products/{productId}", "/products/{productId}/reviews"})
    public ResponseEntity<ReviewDto> createReview(@PathVariable Long productId, 
                                                  @Valid @RequestBody ReviewDto reviewDto,
                                                  @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                  @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            ReviewDto created = reviewService.createReview(productId, reviewDto, userId, username);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create review: " + e.getMessage());
        }
    }
    
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(@PathVariable Long reviewId,
                                                 @Valid @RequestBody ReviewDto reviewDto,
                                                 @RequestHeader("X-User-Id") Long userId) {
        try {
            ReviewDto updated = reviewService.updateReview(reviewId, reviewDto, userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            throw new BadRequestException("Failed to update review: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId,
                                              @RequestHeader("X-User-Id") Long userId) {
        try {
            reviewService.deleteReview(reviewId, userId);
            return ResponseEntity.ok("Review deleted successfully");
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete review: " + e.getMessage());
        }
    }
    
    @GetMapping({"/reviews/products/{productId}", "/products/{productId}/reviews"})
    public ResponseEntity<List<ReviewDto>> getProductReviews(@PathVariable Long productId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ReviewDto> reviewPage = reviewService.getProductReviews(productId, page, size);
            return ResponseEntity.ok(reviewPage.getContent());
        } catch (Exception e) {
            throw new BadRequestException("Failed to get product reviews: " + e.getMessage());
        }
    }
    
    @GetMapping({"/reviews/products/{productId}/summary", "/products/{productId}/reviews/summary"})
    public ResponseEntity<ReviewSummaryDto> getProductReviewSummary(@PathVariable Long productId) {
        try {
            ReviewSummaryDto summary = reviewService.getProductReviewSummary(productId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get review summary: " + e.getMessage());
        }
    }
    
    @GetMapping({"/reviews/users/{userId}", "/users/{userId}/reviews"})
    public ResponseEntity<List<ReviewDto>> getUserReviews(@PathVariable Long userId) {
        try {
            List<ReviewDto> reviews = reviewService.getUserReviews(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get user reviews: " + e.getMessage());
        }
    }
    
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> getReview(@PathVariable Long reviewId) {
        try {
            ReviewDto review = reviewService.getReview(reviewId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get review: " + e.getMessage());
        }
    }
    
    // Admin endpoints
    @GetMapping("/reviews/admin/all")
    public ResponseEntity<Page<ReviewDto>> getAllReviews(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ReviewDto> reviews = reviewService.getAllReviews(page, size);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get all reviews: " + e.getMessage());
        }
    }
    
    @PostMapping("/reviews/admin/{reviewId}/approve")
    public ResponseEntity<ReviewDto> approveReview(@PathVariable Long reviewId) {
        try {
            ReviewDto review = reviewService.approveReview(reviewId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            throw new BadRequestException("Failed to approve review: " + e.getMessage());
        }
    }
    
    @PostMapping("/reviews/admin/{reviewId}/reject")
    public ResponseEntity<ReviewDto> rejectReview(@PathVariable Long reviewId) {
        try {
            ReviewDto review = reviewService.rejectReview(reviewId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            throw new BadRequestException("Failed to reject review: " + e.getMessage());
        }
    }
}
