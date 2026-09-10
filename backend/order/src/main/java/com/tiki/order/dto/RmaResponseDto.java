package com.tiki.order.dto;

import com.tiki.order.entity.RmaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmaResponseDto {
    private Long id;
    private String rmaNumber;
    private Integer orderId;
    private Long userId;
    private Long shopId;
    private RmaEntity.RmaType type;
    private RmaEntity.ReasonCategory reasonCategory;
    private String reason;
    private RmaEntity.RmaStatus status;
    private String customerNote;
    private String sellerNote;
    private String adminNote;
    private String returnTrackingNumber;
    private String returnCarrier;
    private Integer exchangeOrderId;
    private BigDecimal refundAmount;
    private RmaEntity.RefundMethod refundMethod;
    private RmaEntity.InspectionResult inspectionResult;
    private String inspectionNote;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<RmaItemDto> items;
}
