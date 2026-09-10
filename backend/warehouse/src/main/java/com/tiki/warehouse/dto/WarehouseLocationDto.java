package com.tiki.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseLocationDto {
    private Long id;
    private String code;
    private String name;
    private String province;
    private String address;
    private Integer capacity;
    private Boolean isActive;
}
