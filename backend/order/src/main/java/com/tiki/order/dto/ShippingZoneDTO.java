package com.tiki.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShippingZoneDTO {
    private Integer id;
    private String name;
    private List<String> provinces;
    private BigDecimal fee;
    private Integer estimatedDays;
    private String displayName;
    private Boolean isActive;
}
