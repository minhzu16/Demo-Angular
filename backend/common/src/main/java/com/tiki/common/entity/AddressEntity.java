package com.tiki.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "addresses")
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 50)
    private String label;

    // Lưu JSON dạng chuỗi, tuỳ ORM có thể map JsonType nhưng để đơn giản dùng String
    @Column(name = "json_address", columnDefinition = "json")
    private String jsonAddress;

    @Column(name = "is_default")
    private Boolean isDefault;
}
