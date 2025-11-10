package com.tiki.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoiceDto {
    private Long id;
    private Integer orderId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private LocalDateTime issuedAt;
    private String filePath;
}
