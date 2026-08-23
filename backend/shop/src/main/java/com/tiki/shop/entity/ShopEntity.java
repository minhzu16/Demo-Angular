package com.tiki.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private Long sellerId; // Link to user in auth service
    
    @Column(name = "shop_name", unique = true)
    private String name;
    
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
    
    private String description;
    
    private String logoUrl;
    
    private String bannerUrl;
    
    private String address;
    
    private boolean isActive;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
