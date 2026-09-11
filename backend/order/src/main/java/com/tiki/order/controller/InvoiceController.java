package com.tiki.order.controller;

import com.tiki.order.dto.InvoiceDto;
import com.tiki.order.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@CrossOrigin
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;
    
    /**
     * Get invoice by order ID (v1 API - Public/Buyer)
     * GET /api/v1/invoices/order/{orderId}
     */
    @GetMapping("/api/v1/invoices/order/{orderId}")
    public ResponseEntity<InvoiceDto> getInvoiceByOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByOrder(orderId));
    }

    /**
     * Get all invoices for a seller's shop
     * GET /api/v1/invoices/shop/{shopId}
     */
    @GetMapping("/api/v1/invoices/shop/{shopId}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByShop(shopId));
    }

    @PostMapping("/api/admin/invoices/issue")
    public ResponseEntity<InvoiceDto> issue(@RequestParam Integer orderId,
                                            @RequestParam BigDecimal totalAmount) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.issueInvoice(orderId,totalAmount));
    }

    @GetMapping("/api/admin/invoices")
    public ResponseEntity<List<InvoiceDto>> getAll(){
        return ResponseEntity.ok(invoiceService.getAll());
    }

    @GetMapping("/api/admin/invoices/{id}")
    public ResponseEntity<InvoiceDto> getById(@PathVariable Long id){
        return invoiceService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
