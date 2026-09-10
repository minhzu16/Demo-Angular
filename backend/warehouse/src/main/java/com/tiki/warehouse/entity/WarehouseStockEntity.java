package com.tiki.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_stocks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_warehouse_product", columnNames = {"warehouse_id", "product_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Builder.Default
    private Integer quantity = 0;

    @Builder.Default
    private Integer reservedQuantity = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public int getAvailableQuantity() {
        int q = quantity != null ? quantity : 0;
        int r = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, q - r);
    }
}
