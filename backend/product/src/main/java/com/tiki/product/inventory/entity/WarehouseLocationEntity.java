package com.tiki.product.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouse_locations")
public class WarehouseLocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private WarehouseEntity warehouse;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public WarehouseEntity getWarehouse() { return warehouse; }
    public void setWarehouse(WarehouseEntity warehouse) { this.warehouse = warehouse; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
