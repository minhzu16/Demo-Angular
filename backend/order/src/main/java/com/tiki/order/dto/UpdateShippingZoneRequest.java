package com.tiki.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateShippingZoneRequest {

    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    private List<String> provinces;

    @DecimalMin(value = "0", message = "Fee must be >= 0")
    private BigDecimal fee;

    @Min(value = 1, message = "Estimated days must be at least 1")
    private Integer estimatedDays;

    private Boolean isActive;
}
