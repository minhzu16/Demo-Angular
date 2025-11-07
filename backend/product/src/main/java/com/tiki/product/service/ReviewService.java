package com.tiki.product.service;

import com.tiki.product.dto.ReviewDto;
import com.tiki.product.dto.ReviewSummaryDto;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.entity.Review;
import com.tiki.product.repository.ProductRepository;
import com.tiki.product.repository.ReviewRepository;
import com.tiki.product.client.UserClient;
import com.tiki.product.exception.BadRequestException;
import com.tiki.product.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserClient userClient;
    
    public ReviewDto createReview(Long productId, ReviewDto reviewDto, Long userId, String username) {
        ProductEntity product = productRepository.findById(productId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        // If headers not provided, treat as anonymous
        if (userId == null) {
            userId = 0L;
        }
        if (username == null || username.isBlank()) {
            username = "anonymous";
        }

        // Validate user (if not anonymous)
        if (userId != 0) {
            try {
                userClient.getUser(userId);
            } catch (Exception ex) {
                throw new BadRequestException("Invalid user id");
            }
        }
        // Check if user already reviewed this product (skip check for anonymous)
        if (userId != 0 && reviewRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
            throw new BadRequestException("You have already reviewed this product");
        }
        
        Review review = new Review();
        review.setProduct(product);
        review.setUserId(userId);
        review.setUsername(username);
        review.setRating(reviewDto.getRating());
        review.setTitle(reviewDto.getTitle());
        review.setContent(reviewDto.getContent());
        review.setIsVerifiedPurchase(reviewDto.getIsVerifiedPurchase());
        
        review = reviewRepository.save(review);
        return convertToDto(review);
    }
    
    public ReviewDto updateReview(Long reviewId, ReviewDto reviewDto, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("You can only update your own reviews");
        }
        
        review.setRating(reviewDto.getRating());
        review.setTitle(reviewDto.getTitle());
        review.setContent(reviewDto.getContent());
        
        review = reviewRepository.save(review);
        return convertToDto(review);
    }
    
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }
        
        reviewRepository.delete(review);
    }
    
    public Page<ReviewDto> getProductReviews(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId, pageable);
        return reviews.map(this::convertToDto);
    }
    
    public List<ReviewDto> getUserReviews(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return reviews.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    public ReviewSummaryDto getProductReviewSummary(Long productId) {
        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);
        Long totalReviews = reviewRepository.getReviewCountByProductId(productId);
        
        List<Object[]> ratingDistributionData = reviewRepository.getRatingDistributionByProductId(productId);
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (Object[] data : ratingDistributionData) {
            ratingDistribution.put((Integer) data[0], (Long) data[1]);
        }
        
        List<Review> recentReviews = reviewRepository.findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId)
                .stream().limit(5).collect(Collectors.toList());
        List<ReviewDto> recentReviewDtos = recentReviews.stream().map(this::convertToDto).collect(Collectors.toList());
        
        return new ReviewSummaryDto(averageRating, totalReviews, ratingDistribution, recentReviewDtos);
    }
    
    public ReviewDto getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        return convertToDto(review);
    }
    
    // Admin methods
    public Page<ReviewDto> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findAll(pageable);
        return reviews.map(this::convertToDto);
    }
    
    public ReviewDto approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        review.setIsApproved(true);
        review = reviewRepository.save(review);
        return convertToDto(review);
    }
    
    public ReviewDto rejectReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        review.setIsApproved(false);
        review = reviewRepository.save(review);
        return convertToDto(review);
    }
    
    private ReviewDto convertToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId().longValue());
        dto.setUserId(review.getUserId());
        dto.setUsername(review.getUsername());
        dto.setRating(review.getRating());
        dto.setTitle(review.getTitle());
        dto.setContent(review.getContent());
        dto.setIsVerifiedPurchase(review.getIsVerifiedPurchase());
        dto.setIsApproved(review.getIsApproved());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}
