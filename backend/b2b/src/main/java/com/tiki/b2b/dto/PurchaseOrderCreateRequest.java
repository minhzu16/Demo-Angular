package com.tiki.b2b.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderCreateRequest {

    @NotNull(message = "Company ID không được để trống")
    private Long companyId;

    @NotEmpty(message = "Danh sách sản phẩm không được rỗng")
    private List<PurchaseOrderItemDto> items;

    private Boolean invoiceRequired;
    private LocalDate deliveryDate;
    private String note;
}
