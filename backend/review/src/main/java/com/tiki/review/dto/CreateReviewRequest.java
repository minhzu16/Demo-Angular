package com.tiki.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateReviewRequest – inbound DTO for review creation.
 * Keeps the entity decoupled from client-supplied input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "productId là bắt buộc")
    private Long productId;

    @NotNull(message = "userId là bắt buộc")
    private Long userId;

    private String userName;

    @Min(value = 1, message = "Rating phải từ 1 đến 5")
    @Max(value = 5, message = "Rating phải từ 1 đến 5")
    private int rating;

    private String comment;
    
    private java.util.List<String> mediaUrls;
}
