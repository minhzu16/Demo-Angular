package com.tiki.order.service;

import com.tiki.order.dto.InvoiceDto;
import com.tiki.order.entity.InvoiceEntity;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.InvoiceRepository;
import com.tiki.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private OrderRepository orderRepo;

    @InjectMocks
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invoiceService, "storagePath", "target/test-invoices");
    }

    @Test
    void testIssueInvoice_Success() throws IOException {
        InvoiceEntity saved = new InvoiceEntity();
        saved.setId(10L);
        saved.setOrderId(100);
        saved.setInvoiceNumber("INV-100");
        saved.setTotalAmount(new BigDecimal("500000.00"));
        saved.setIssuedAt(LocalDateTime.now());

        when(invoiceRepo.save(any(InvoiceEntity.class))).thenReturn(saved);

        InvoiceDto dto = invoiceService.issueInvoice(100, new BigDecimal("500000.00"));

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(100, dto.getOrderId());
        assertEquals(new BigDecimal("500000.00"), dto.getTotalAmount());
        verify(invoiceRepo, times(1)).save(any(InvoiceEntity.class));
    }

    @Test
    void testGetInvoiceByOrder_ExistingInvoice() {
        InvoiceEntity existing = new InvoiceEntity();
        existing.setId(1L);
        existing.setOrderId(101);
        existing.setInvoiceNumber("INV-101");
        existing.setTotalAmount(new BigDecimal("750000.00"));
        existing.setIssuedAt(LocalDateTime.now());

        when(invoiceRepo.findByOrderId(101)).thenReturn(Optional.of(existing));

        InvoiceDto dto = invoiceService.getInvoiceByOrder(101);

        assertNotNull(dto);
        assertEquals("INV-101", dto.getInvoiceNumber());
        assertEquals(new BigDecimal("750000.00"), dto.getTotalAmount());
        verify(orderRepo, never()).findById(any());
    }

    @Test
    void testGetInvoiceByOrder_OrderFound_AutoIssuesInvoice() {
        when(invoiceRepo.findByOrderId(102)).thenReturn(Optional.empty());

        OrderEntity order = new OrderEntity();
        order.setId(102);
        order.setTotalAmount(new BigDecimal("1200000.00"));
        order.setStatus(OrderEntity.OrderStatus.DELIVERED);

        when(orderRepo.findById(102)).thenReturn(Optional.of(order));

        InvoiceEntity saved = new InvoiceEntity();
        saved.setId(2L);
        saved.setOrderId(102);
        saved.setInvoiceNumber("INV-102");
        saved.setTotalAmount(new BigDecimal("1200000.00"));
        saved.setIssuedAt(LocalDateTime.now());

        when(invoiceRepo.save(any(InvoiceEntity.class))).thenReturn(saved);

        InvoiceDto dto = invoiceService.getInvoiceByOrder(102);

        assertNotNull(dto);
        assertEquals(new BigDecimal("1200000.00"), dto.getTotalAmount());
        verify(invoiceRepo, times(1)).save(any(InvoiceEntity.class));
    }

    @Test
    void testGetInvoiceByOrder_OrderNotFound_ThrowsException() {
        when(invoiceRepo.findByOrderId(999)).thenReturn(Optional.empty());
        when(orderRepo.findById(999)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            invoiceService.getInvoiceByOrder(999);
        });

        assertTrue(ex.getMessage().contains("Không tìm thấy đơn hàng"));
    }

    @Test
    void testGetInvoicesByShop_ReturnsMatchingInvoices() {
        OrderEntity order1 = new OrderEntity();
        order1.setId(10);
        order1.setShopId(5L);

        OrderEntity order2 = new OrderEntity();
        order2.setId(20);
        order2.setShopId(5L);

        when(orderRepo.findByShopId(5L)).thenReturn(List.of(order1, order2));

        InvoiceEntity inv1 = new InvoiceEntity();
        inv1.setId(1L);
        inv1.setOrderId(10);
        inv1.setInvoiceNumber("INV-10");
        inv1.setTotalAmount(new BigDecimal("200000.00"));

        InvoiceEntity inv2 = new InvoiceEntity();
        inv2.setId(2L);
        inv2.setOrderId(20);
        inv2.setInvoiceNumber("INV-20");
        inv2.setTotalAmount(new BigDecimal("300000.00"));

        when(invoiceRepo.findByOrderIdIn(List.of(10, 20))).thenReturn(List.of(inv1, inv2));

        List<InvoiceDto> dtos = invoiceService.getInvoicesByShop(5L);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("INV-10", dtos.get(0).getInvoiceNumber());
        assertEquals("INV-20", dtos.get(1).getInvoiceNumber());
    }
}
