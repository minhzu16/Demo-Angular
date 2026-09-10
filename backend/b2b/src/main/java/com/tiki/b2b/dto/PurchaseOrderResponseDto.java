package com.tiki.b2b.dto;

import com.tiki.b2b.entity.PurchaseOrderEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponseDto {
    private Long id;
    private String poNumber;
    private Long companyId;
    private Long requestedBy;
    private Long approvedBy;
    private PurchaseOrderEntity.POStatus status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private Boolean invoiceRequired;
    private LocalDate deliveryDate;
    private String note;
    private String rejectionReason;
    private Integer convertedOrderId;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemDto> items;
}
