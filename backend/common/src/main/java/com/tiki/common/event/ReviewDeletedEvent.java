package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Review Deleted Event
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDeletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long productId;
    private Integer rating;
}
