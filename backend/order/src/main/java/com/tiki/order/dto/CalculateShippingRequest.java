package com.tiki.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CalculateShippingRequest {

    @NotBlank(message = "Province is required")
    private String province;

    private String district;
}
