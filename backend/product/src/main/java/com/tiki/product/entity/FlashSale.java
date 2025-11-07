package com.tiki.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Flash Sale Entity
 * Sprint 16: Flash Sales System
 */
@Entity
@Table(name = "flash_sales", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_start_time", columnList = "start_time"),
    @Index(name = "idx_end_time", columnList = "end_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlashSale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FlashSaleStatus status = FlashSaleStatus.SCHEDULED;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "flashSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlashSaleProduct> products = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if flash sale is currently active
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == FlashSaleStatus.ACTIVE 
            && now.isAfter(startTime) 
            && now.isBefore(endTime);
    }
    
    /**
     * Check if flash sale has ended
     */
    public boolean hasEnded() {
        return status == FlashSaleStatus.ENDED 
            || LocalDateTime.now().isAfter(endTime);
    }
    
    public enum FlashSaleStatus {
        SCHEDULED,  // Chưa bắt đầu
        ACTIVE,     // Đang diễn ra
        ENDED,      // Đã kết thúc
        CANCELLED   // Đã hủy
    }
}
