package com.tiki.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private OrderEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "variant_name")
    private String variantName;
    
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "image_url")
    private String imageUrl;
}
