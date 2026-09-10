package com.tiki.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "complaints")
public class ComplaintEntity {

    public enum Status { PENDING, RESOLVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "text")
    private String resolution;

    @Column(name = "seller_response", columnDefinition = "text")
    private String sellerResponse;

    @Column(name = "seller_response_at")
    private LocalDateTime sellerResponseAt;

    @Column(name = "resolution_type", length = 50)
    private String resolutionType;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
