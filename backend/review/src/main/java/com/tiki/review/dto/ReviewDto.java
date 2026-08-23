package com.tiki.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReviewDto – API-facing representation of a review.
 * Keeps the entity decoupled from the HTTP layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {

    private Long id;
    private Long productId;
    private Long userId;
    private String userName;
    private int rating;
    private String comment;
    private java.util.List<String> mediaUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
