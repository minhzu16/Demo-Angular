package com.tiki.order.dto;

import com.tiki.order.entity.VoucherEntity;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 50, message = "Mã phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã chỉ chứa chữ in hoa, số, gạch dưới và gạch ngang")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private VoucherEntity.DiscountType type;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị phải lớn hơn 0")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu phải >= 0")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải là tương lai")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Số lần sử dụng tối đa phải ít nhất là 1")
    private Integer maxUsage = 1000;

    private Boolean isActive = true;
}
