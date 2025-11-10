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

    @Value("${invoice.storage-path:template_storage/invoices}")
    private String storagePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        Path dir = Paths.get(storagePath);
        if(!Files.exists(dir)){
            Files.createDirectories(dir);
        }
        Path file = buildFilePath(invoiceNumber);
        objectMapper.writeValue(file.toFile(), toDto(e));

        return toDto(e);
    }

    public List<InvoiceDto> getAll(){
        return invoiceRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<InvoiceDto> getById(Long id){
        return invoiceRepo.findById(id).map(this::toDto);
    }
}
