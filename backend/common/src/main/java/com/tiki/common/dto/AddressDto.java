package com.tiki.common.dto;

import lombok.Data;

@Data
public class AddressDto {
    private Integer id;
    private Long userId;
    private String label;
    private String jsonAddress;
    private Boolean isDefault;
}
