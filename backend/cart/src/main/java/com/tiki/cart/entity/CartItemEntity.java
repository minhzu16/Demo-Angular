package com.tiki.cart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    private Double priceSnapshot;
}
