package com.tiki.review.service;

import com.tiki.review.dto.ReviewDto;
import com.tiki.review.entity.ReviewEntity;
import com.tiki.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewQueryService – unit tests")
class ReviewQueryServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Spy
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewQueryService queryService;

    private ReviewEntity buildReview(Long productId, Long userId, int rating) {
        return ReviewEntity.builder()
                .id(userId)          // reuse for simplicity
                .productId(productId)
                .userId(userId)
                .userName("User " + userId)
                .rating(rating)
                .comment("Comment")
                .build();
    }

    // ── getProductReviews ────────────────────────────────────────────────────

    @Test
    @DisplayName("getProductReviews – returns mapped DTOs in order")
    void getProductReviews_ReturnsMappedPage() {
        ReviewEntity r1 = buildReview(10L, 1L, 5);
        ReviewEntity r2 = buildReview(10L, 2L, 3);
        Page<ReviewEntity> entityPage = new PageImpl<>(List.of(r1, r2));

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                .thenReturn(entityPage);

        Page<ReviewDto> result = queryService.getProductReviews(10L, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getRating()).isEqualTo(5);
        assertThat(result.getContent().get(1).getRating()).isEqualTo(3);
    }

    @Test
    @DisplayName("getProductReviews – empty product returns empty page")
    void getProductReviews_EmptyProduct_ReturnsEmptyPage() {
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(eq(99L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ReviewDto> result = queryService.getProductReviews(99L, 0, 10);

        assertThat(result.getTotalElements()).isZero();
    }

    // ── getProductReviewStats ────────────────────────────────────────────────

    @Test
    @DisplayName("getProductReviewStats – calculates correct average")
    void getProductReviewStats_CorrectAverage() {
        ReviewEntity r1 = buildReview(10L, 1L, 5);
        ReviewEntity r2 = buildReview(10L, 2L, 3);
        Page<ReviewEntity> page = new PageImpl<>(List.of(r1, r2));

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                .thenReturn(page);

        Map<String, Object> stats = queryService.getProductReviewStats(10L);

        assertThat(stats.get("totalReviews")).isEqualTo(2);
        assertThat(stats.get("averageRating")).isEqualTo(4.0);
    }

    @Test
    @DisplayName("getProductReviewStats – no reviews returns 0 average")
    void getProductReviewStats_NoReviews_ReturnsZeroAverage() {
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Map<String, Object> stats = queryService.getProductReviewStats(10L);

        assertThat(stats.get("totalReviews")).isEqualTo(0);
        assertThat(stats.get("averageRating")).isEqualTo(0.0);
    }

    // ── getUserReviews ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserReviews – returns all reviews for the user")
    void getUserReviews_ReturnsUserReviews() {
        ReviewEntity r1 = buildReview(10L, 1L, 4);
        ReviewEntity r2 = buildReview(11L, 1L, 5);
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r1, r2));

        List<ReviewDto> result = queryService.getUserReviews(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserReviews – user with no reviews returns empty list")
    void getUserReviews_NoReviews_ReturnsEmptyList() {
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(99L)).thenReturn(List.of());

        List<ReviewDto> result = queryService.getUserReviews(99L);

        assertThat(result).isEmpty();
    }
}
