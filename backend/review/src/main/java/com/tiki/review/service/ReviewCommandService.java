package com.tiki.review.service;

import com.tiki.common.config.RabbitMQConfig;
import com.tiki.common.event.ReviewCreatedEvent;
import com.tiki.review.client.OrderClient;
import com.tiki.review.dto.CreateReviewRequest;
import com.tiki.review.dto.ReviewDto;
import com.tiki.review.entity.ReviewEntity;
import com.tiki.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReviewCommandService – Single-Responsibility: mutating operations on reviews.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Validate that the user has actually purchased the product (via OrderClient)</li>
 *   <li>Persist a new review</li>
 *   <li>Publish a {@link ReviewCreatedEvent} over RabbitMQ for downstream consumers</li>
 *   <li>Delete an existing review</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;
    private final RabbitTemplate rabbitTemplate;
    private final ReviewMapper reviewMapper;

    /**
     * Creates a review after verifying that the user has purchased and received the product.
     *
     * @param request inbound DTO with review details
     * @return persisted review as a DTO
     * @throws RuntimeException if the purchase cannot be verified or the user hasn't purchased
     */
    @Transactional
    public ReviewDto createReview(CreateReviewRequest request) {
        log.info("Creating review for product {} by user {}", request.getProductId(), request.getUserId());

        // 1. Verify purchase
        verifyPurchase(request.getProductId(), request.getUserId());

        // 2. ✅ BUG 29 FIX: Prevent duplicate reviews — one user = one review per product
        if (reviewRepository.existsByUserIdAndProductId(request.getUserId(), request.getProductId())) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi. Mỗi sản phẩm chỉ được đánh giá một lần.");
        }

        // 3. Persist
        ReviewEntity entity = reviewMapper.toEntity(request);
        ReviewEntity saved = reviewRepository.save(entity);
        log.info("Review saved with id={}", saved.getId());

        // 4. Publish event (non-blocking; failure is logged but does not roll back)
        publishReviewCreatedEvent(saved);

        return reviewMapper.toDto(saved);
    }

    /**
     * Deletes a review by its id.
     *
     * @param reviewId the review to delete
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        log.info("Deleting review id={}", reviewId);
        reviewRepository.findById(reviewId).ifPresent(review -> {
            reviewRepository.deleteById(reviewId);
            publishReviewDeletedEvent(review);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void verifyPurchase(Long productId, Long userId) {
        Boolean hasPurchased;
        try {
            hasPurchased = orderClient.checkUserPurchasedProduct(productId, userId);
        } catch (Exception ex) {
            log.error("Could not verify purchase for user {} on product {}: {}",
                    userId, productId, ex.getMessage());
            throw new RuntimeException("Không thể xác minh đơn hàng. Vui lòng thử lại sau.");
        }

        if (hasPurchased == null || !hasPurchased) {
            throw new RuntimeException(
                    "Bạn phải mua sản phẩm này và nhận hàng thành công mới được đánh giá.");
        }
    }

    private void publishReviewCreatedEvent(ReviewEntity saved) {
        try {
            ReviewCreatedEvent event = ReviewCreatedEvent.builder()
                    .id(saved.getId())
                    .productId(saved.getProductId())
                    .userId(saved.getUserId())
                    .rating(saved.getRating())
                    .comment(saved.getComment())
                    .createdAt(saved.getCreatedAt())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.REVIEW_EXCHANGE,
                    RabbitMQConfig.REVIEW_CREATED_KEY,
                    event);
            log.info("Published ReviewCreatedEvent for product {}", saved.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish ReviewCreatedEvent for review {}", saved.getId(), e);
        }
    }

    private void publishReviewDeletedEvent(ReviewEntity review) {
        try {
            com.tiki.common.event.ReviewDeletedEvent event = com.tiki.common.event.ReviewDeletedEvent.builder()
                    .id(review.getId())
                    .productId(review.getProductId())
                    .rating(review.getRating())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.REVIEW_EXCHANGE,
                    RabbitMQConfig.REVIEW_DELETED_KEY,
                    event);
            log.info("Published ReviewDeletedEvent for product {}", review.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish ReviewDeletedEvent for review {}", review.getId(), e);
        }
    }
}
