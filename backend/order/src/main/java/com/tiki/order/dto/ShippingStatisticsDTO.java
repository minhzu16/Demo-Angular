package com.tiki.order.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShippingStatisticsDTO {
    private Integer totalZones;
    private Integer activeZones;
    private Integer totalProvincesCovered;
    private List<String> coveredProvinces;
}
