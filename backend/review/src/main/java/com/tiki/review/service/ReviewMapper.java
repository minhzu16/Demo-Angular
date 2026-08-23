package com.tiki.review.service;

import com.tiki.review.dto.CreateReviewRequest;
import com.tiki.review.dto.ReviewDto;
import com.tiki.review.entity.ReviewEntity;
import org.springframework.stereotype.Component;

/**
 * ReviewMapper – single-responsibility component for converting between
 * ReviewEntity and ReviewDto / CreateReviewRequest.
 */
@Component
public class ReviewMapper {

    public ReviewDto toDto(ReviewEntity entity) {
        if (entity == null) return null;
        return ReviewDto.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .mediaUrls(entity.getMediaUrls())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ReviewEntity toEntity(CreateReviewRequest request) {
        if (request == null) return null;
        return ReviewEntity.builder()
                .productId(request.getProductId())
                .userId(request.getUserId())
                .userName(request.getUserName())
                .rating(request.getRating())
                .comment(request.getComment())
                .mediaUrls(request.getMediaUrls())
                .build();
    }
}
