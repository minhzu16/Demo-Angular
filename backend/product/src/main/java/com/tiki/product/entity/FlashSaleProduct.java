package com.tiki.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flash Sale Product Entity
 * Sprint 16: Flash Sales System
 */
@Entity
@Table(name = "flash_sale_products", 
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_flash_sale_product", columnNames = {"flash_sale_id", "product_id"})
    },
    indexes = {
        @Index(name = "idx_flash_sale_id", columnList = "flash_sale_id"),
        @Index(name = "idx_product_id", columnList = "product_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_sale_id", nullable = false)
    private FlashSale flashSale;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;
    
    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(name = "quantity_limit", nullable = false)
    private Integer quantityLimit = 0;
    
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold = 0;
    
    @Column(name = "max_per_user", nullable = false)
    private Integer maxPerUser = 1;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Calculate discount percentage if not set
        if (discountPercentage == null && originalPrice != null && salePrice != null) {
            BigDecimal discount = originalPrice.subtract(salePrice);
            discountPercentage = discount.divide(originalPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if product is still available for purchase
     */
    public boolean isAvailable() {
        return quantitySold < quantityLimit;
    }
    
    /**
     * Get remaining quantity
     */
    public Integer getRemainingQuantity() {
        return quantityLimit - quantitySold;
    }
    
    /**
     * Check if user can purchase more
     */
    public boolean canPurchase(int currentUserQuantity, int requestedQuantity) {
        return (currentUserQuantity + requestedQuantity) <= maxPerUser 
            && (quantitySold + requestedQuantity) <= quantityLimit;
    }
}
