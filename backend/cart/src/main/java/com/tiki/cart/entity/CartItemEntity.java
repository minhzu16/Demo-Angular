package com.tiki.cart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private CartEntity cart;

    private Integer productId;

    @Column(name = "variant_id")
    private Integer variantId;

    @Column(name = "qty")
    private Integer quantity = 0;

    @Column(name = "price_snapshot", precision = 19, scale = 2)
    private BigDecimal priceSnapshot;
}
