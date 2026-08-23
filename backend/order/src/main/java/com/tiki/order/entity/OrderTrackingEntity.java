package com.tiki.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_tracking")
public class OrderTrackingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderEntity.OrderStatus status;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public OrderTrackingEntity(Integer orderId, OrderEntity.OrderStatus status, String note) {
        this.orderId = orderId;
        this.status = status;
        this.note = note;
    }
}
