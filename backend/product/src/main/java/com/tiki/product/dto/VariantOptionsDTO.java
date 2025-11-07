package com.tiki.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for available variant options (colors and sizes)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantOptionsDTO {
    private List<String> availableColors;
    private List<String> availableSizes;
}
