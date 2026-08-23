package com.tiki.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long productId;
    
    private Long userId;
    
    private String userName; // Denormalized for quick display
    
    private int rating; // 1-5
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    // Optional: photos, verified purchase, etc.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_media", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "media_url", length = 1000)
    private java.util.List<String> mediaUrls;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
