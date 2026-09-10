package com.tiki.order.dto;

import com.tiki.order.entity.RmaEntity;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RmaCreateRequest {

    @NotNull(message = "Mã đơn hàng không được để trống")
    private Integer orderId;

    private Long shopId;

    @NotNull(message = "Loại yêu cầu RMA không được để trống")
    private RmaEntity.RmaType type;

    @NotNull(message = "Lý do RMA không được để trống")
    private RmaEntity.ReasonCategory reasonCategory;

    private String reason;
    private String customerNote;

    private BigDecimal requestedRefundAmount;
    private RmaEntity.RefundMethod refundMethod;

    @NotEmpty(message = "Danh sách sản phẩm trả về không được rỗng")
    private List<RmaItemDto> items;
}
