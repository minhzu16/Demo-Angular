package com.tiki.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerComplaintResponseRequest {
    @NotBlank(message = "Nội dung phản hồi của người bán không được để trống")
    private String response;
}
