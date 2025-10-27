package com.tiki.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Created Event
 * Sprint 9 - Day 7-9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer orderId;
    private Integer userId;
    private Integer shopId;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String userEmail;
    private String userPhone;
    private LocalDateTime createdAt;
}
