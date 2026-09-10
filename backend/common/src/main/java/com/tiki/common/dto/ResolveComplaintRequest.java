package com.tiki.common.dto;

import com.tiki.common.entity.ComplaintEntity;
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
public class ResolveComplaintRequest {

    @NotNull(message = "Trạng thái giải quyết không được để trống")
    private ComplaintEntity.Status status;

    @NotBlank(message = "Nội dung giải quyết/phản hồi không được để trống")
    private String resolution;

    private String resolutionType; // REFUND, REJECT, COMPENSATION
}
