package com.tiki.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wishlist Entity
 * Sprint 10 - Wishlist Feature
 */
@Entity
@Table(name = "wishlists", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_product_variant",
        columnNames = {"user_id", "product_id", "variant_id"}
    ),
    indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_product", columnList = "product_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "variant_id")
    private Long variantId;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    @Column(name = "added_at")
    private LocalDateTime addedAt;
    
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
