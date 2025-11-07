package com.tiki.product.inventory.entity;

import com.tiki.product.entity.ProductEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory_transaction_items")
public class InventoryTransactionItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private InventoryTransactionEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocationEntity location;

    @Column(nullable = false)
    private Integer quantity;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public InventoryTransactionEntity getTransaction() { return transaction; }
    public void setTransaction(InventoryTransactionEntity transaction) { this.transaction = transaction; }
    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }
    public WarehouseLocationEntity getLocation() { return location; }
    public void setLocation(WarehouseLocationEntity location) { this.location = location; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
