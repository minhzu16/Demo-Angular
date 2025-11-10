package com.tiki.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOptionDTO {
    private Integer zoneId;
    private String zoneName;
    private BigDecimal fee;
    private Integer estimatedDays;
    private String displayText;

    public static ShippingOptionDTO fromZone(ShippingZoneDTO zone) {
        return new ShippingOptionDTO(
                zone.getId(),
                zone.getName(),
                zone.getFee(),
                zone.getEstimatedDays(),
                zone.getDisplayName()
        );
    }
}
