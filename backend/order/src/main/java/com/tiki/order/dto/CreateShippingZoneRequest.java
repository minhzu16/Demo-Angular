package com.tiki.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateShippingZoneRequest {

    @NotBlank(message = "Zone name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotEmpty(message = "At least one province is required")
    private List<@NotBlank(message = "Province name cannot be blank") String> provinces;

    @NotNull(message = "Shipping fee is required")
    @DecimalMin(value = "0", message = "Fee must be >= 0")
    private BigDecimal fee;

    @Min(value = 1, message = "Estimated days must be at least 1")
    private Integer estimatedDays = 2;

    private Boolean isActive = true;
}
