package com.tiki.review.controller;

import com.tiki.review.dto.CreateReviewRequest;
import com.tiki.review.dto.ReviewDto;
import com.tiki.review.service.ReviewCommandService;
import com.tiki.review.service.ReviewQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ReviewController – thin HTTP adapter.
 * Delegates entirely to {@link ReviewQueryService} and {@link ReviewCommandService}.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewQueryService reviewQueryService;
    private final ReviewCommandService reviewCommandService;

    /** GET /api/v1/reviews/product/{productId}?page=0&size=10 */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewDto>> getByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET reviews for product={}, page={}, size={}", productId, page, size);
        return ResponseEntity.ok(reviewQueryService.getProductReviews(productId, page, size));
    }

    /** GET /api/v1/reviews/product/{productId}/stats */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductStats(@PathVariable Long productId) {
        log.info("GET review stats for product={}", productId);
        return ResponseEntity.ok(reviewQueryService.getProductReviewStats(productId));
    }

    /** GET /api/v1/reviews/user/{userId} */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDto>> getByUser(@PathVariable Long userId) {
        log.info("GET reviews for user={}", userId);
        return ResponseEntity.ok(reviewQueryService.getUserReviews(userId));
    }

    /** POST /api/v1/reviews/create */
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReviewDto> create(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        
        if (currentUserId == null) {
            log.warn("SECURITY: Attempt to create review without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // ✅ BUG 13 FIX: Ép buộc userId từ JWT header (tránh IDOR)
        request.setUserId(currentUserId);
        
        log.info("POST create review for product={} by user={}", request.getProductId(), request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewCommandService.createReview(request));
    }

    /** DELETE /api/v1/reviews/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE review id={}", id);
        reviewCommandService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
