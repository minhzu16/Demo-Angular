package com.tiki.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {

    @NotNull(message = "Mã đơn hàng là bắt buộc")
    private Long orderId;

    @NotBlank(message = "Tiêu đề khiếu nại không được để trống")
    private String title;

    @NotBlank(message = "Nội dung chi tiết khiếu nại không được để trống")
    private String description;
}
