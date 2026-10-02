package com.tiki.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_idempotency_keys", indexes = {
        @Index(name = "idx_idemp_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_idemp_expires", columnList = "expires_at")
})
public class OrderIdempotencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
