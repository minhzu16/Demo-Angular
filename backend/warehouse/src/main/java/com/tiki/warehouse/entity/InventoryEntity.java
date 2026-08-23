package com.tiki.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private Long productId;
    
    private Long shopId;
    
    private Integer quantity;
    
    private Integer reservedQuantity; // Quantity in pending orders
    
    private String location; // Warehouse shelf/bin ID
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
