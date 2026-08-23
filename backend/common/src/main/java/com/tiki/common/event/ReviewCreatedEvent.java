package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Review Created Event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
