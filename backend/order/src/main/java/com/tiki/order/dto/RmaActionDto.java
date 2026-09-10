package com.tiki.order.dto;

import com.tiki.order.entity.RmaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmaActionDto {
    private String note;
    private String reason;
    private String trackingNumber;
    private String carrier;
    private RmaEntity.InspectionResult inspectionResult;
}
