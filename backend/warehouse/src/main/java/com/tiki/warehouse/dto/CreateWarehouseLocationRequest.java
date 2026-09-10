package com.tiki.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWarehouseLocationRequest {
    @NotBlank(message = "Mã kho không được để trống")
    private String code;

    @NotBlank(message = "Tên kho không được để trống")
    private String name;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    private String address;
    private Integer capacity;
}
