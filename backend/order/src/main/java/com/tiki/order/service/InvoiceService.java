package com.tiki.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.order.dto.InvoiceDto;
import com.tiki.order.entity.InvoiceEntity;
import com.tiki.order.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepo;

    @Autowired
    private com.tiki.order.repository.OrderRepository orderRepo;

    @Value("${invoice.storage-path:template_storage/invoices}")
    private String storagePath;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private InvoiceDto toDto(InvoiceEntity e){
        InvoiceDto d = new InvoiceDto();
        d.setId(e.getId());
        d.setOrderId(e.getOrderId());
        d.setInvoiceNumber(e.getInvoiceNumber());
        d.setTotalAmount(e.getTotalAmount());
        d.setIssuedAt(e.getIssuedAt());
        d.setFilePath(buildFilePath(e.getInvoiceNumber()).toString());
        return d;
    }

    private Path buildFilePath(String invoiceNumber){
        return Paths.get(storagePath, "invoice_" + invoiceNumber + ".json");
    }

    public InvoiceDto issueInvoice(Integer orderId, BigDecimal totalAmount) throws IOException {
        String invoiceNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        InvoiceEntity e = new InvoiceEntity();
        e.setOrderId(orderId);
        e.setInvoiceNumber(invoiceNumber);
        e.setTotalAmount(totalAmount);
        e.setIssuedAt(LocalDateTime.now());
        e = invoiceRepo.save(e);

        // write file to storage
        try {
            Path dir = Paths.get(storagePath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = buildFilePath(invoiceNumber);
            objectMapper.writeValue(file.toFile(), toDto(e));
        } catch (Exception ex) {
            // Log warning but allow invoice issuance to complete
            org.slf4j.LoggerFactory.getLogger(InvoiceService.class)
                    .warn("Could not persist invoice file: {}", ex.getMessage());
        }

        return toDto(e);
    }

    public InvoiceDto getInvoiceByOrder(Integer orderId) {
        Optional<InvoiceEntity> existing = invoiceRepo.findByOrderId(orderId);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        // Check if order exists in system
        com.tiki.order.entity.OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + orderId));

        try {
            return issueInvoice(orderId, order.getTotalAmount());
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi xuất hóa đơn cho đơn hàng " + orderId, e);
        }
    }

    public List<InvoiceDto> getInvoicesByShop(Long shopId) {
        List<com.tiki.order.entity.OrderEntity> shopOrders = orderRepo.findByShopId(shopId);
        if (shopOrders.isEmpty()) {
            return List.of();
        }
        List<Integer> orderIds = shopOrders.stream()
                .map(com.tiki.order.entity.OrderEntity::getId)
                .collect(Collectors.toList());
        return invoiceRepo.findByOrderIdIn(orderIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<InvoiceDto> getAll(){
        return invoiceRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<InvoiceDto> getById(Long id){
        return invoiceRepo.findById(id).map(this::toDto);
    }
}
