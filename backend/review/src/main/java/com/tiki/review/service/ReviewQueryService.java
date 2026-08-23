package com.tiki.review.service;

import com.tiki.review.dto.ReviewDto;
import com.tiki.review.entity.ReviewEntity;
import com.tiki.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReviewQueryService – Single-Responsibility: read-only access to reviews.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Paginated reviews per product</li>
 *   <li>Aggregate statistics (avg rating, count) per product</li>
 *   <li>Reviews per user</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Returns a page of reviews for the given product, newest first.
     */
    public Page<ReviewDto> getProductReviews(Long productId, int page, int size) {
        log.debug("Fetching reviews for product {}, page={}, size={}", productId, page, size);
        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(page, size))
                .map(reviewMapper::toDto);
    }

    /**
     * Returns aggregate statistics (totalReviews, averageRating) for a product.
     */
    public Map<String, Object> getProductReviewStats(Long productId) {
        log.debug("Calculating review stats for product {}", productId);
        List<ReviewEntity> reviews = reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(0, 1000))
                .getContent();
        double avg = reviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(0.0);
        return Map.of(
                "totalReviews", reviews.size(),
                "averageRating", avg
        );
    }

    /**
     * Returns all reviews written by a specific user, newest first.
     */
    public List<ReviewDto> getUserReviews(Long userId) {
        log.debug("Fetching reviews for user {}", userId);
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(reviewMapper::toDto)
                .collect(Collectors.toList());
    }
}
