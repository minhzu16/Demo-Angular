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

    @Column(name = "verified_purchase")
    @Builder.Default
    private boolean verifiedPurchase = false;

    @Column(name = "shop_reply", columnDefinition = "TEXT")
    private String shopReply;

    @Column(name = "shop_reply_at")
    private LocalDateTime shopReplyAt;

    @Column(name = "shop_reply_user_id")
    private Long shopReplyUserId;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
