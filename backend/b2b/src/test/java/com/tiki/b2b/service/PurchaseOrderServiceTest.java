package com.tiki.b2b.service;

import com.tiki.b2b.dto.PurchaseOrderCreateRequest;
import com.tiki.b2b.dto.PurchaseOrderItemDto;
import com.tiki.b2b.dto.PurchaseOrderResponseDto;
import com.tiki.b2b.entity.CompanyEntity;
import com.tiki.b2b.entity.CompanyUserEntity;
import com.tiki.b2b.entity.PurchaseOrderEntity;
import com.tiki.b2b.repository.CompanyRepository;
import com.tiki.b2b.repository.CompanyUserRepository;
import com.tiki.b2b.repository.PurchaseOrderItemRepository;
import com.tiki.b2b.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyUserRepository companyUserRepository;

    @Mock
    private B2BService b2bService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private CompanyEntity activeCompany;
    private CompanyUserEntity buyerUser;
    private CompanyUserEntity approverUser;

    @BeforeEach
    void setUp() {
        activeCompany = CompanyEntity.builder()
                .id(1L)
                .companyName("Active Corp")
                .status(CompanyEntity.CompanyStatus.ACTIVE)
                .build();

        buyerUser = CompanyUserEntity.builder()
                .companyId(1L)
                .userId(10L)
                .role(CompanyUserEntity.CompanyRole.BUYER)
                .isActive(true)
                .build();

        approverUser = CompanyUserEntity.builder()
                .companyId(1L)
                .userId(20L)
                .role(CompanyUserEntity.CompanyRole.APPROVER)
                .isActive(true)
                .build();
    }

    @Test
    void testCreatePO_Success() {
        PurchaseOrderCreateRequest req = PurchaseOrderCreateRequest.builder()
                .companyId(1L)
                .items(List.of(
                        PurchaseOrderItemDto.builder()
                                .productId(101L)
                                .productName("Sách giáo trình sỉ")
                                .quantity(50)
                                .defaultPrice(new BigDecimal("100000.00"))
                                .build()
                ))
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(activeCompany));
        when(companyUserRepository.findByCompanyIdAndUserIdAndIsActiveTrue(1L, 10L))
                .thenReturn(Optional.of(buyerUser));
        when(b2bService.getB2BPrice(eq(101L), eq(50), any())).thenReturn(new BigDecimal("80000.00"));
        when(purchaseOrderRepository.save(any(PurchaseOrderEntity.class))).thenAnswer(inv -> {
            PurchaseOrderEntity po = inv.getArgument(0);
            po.setId(100L);
            return po;
        });
        when(purchaseOrderItemRepository.saveAll(any())).thenReturn(Collections.emptyList());

        PurchaseOrderResponseDto res = purchaseOrderService.createPO(10L, req);

        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals(new BigDecimal("4000000.00"), res.getTotalAmount()); // 50 * 80k = 4,000,000
        assertEquals(new BigDecimal("320000.00"), res.getTaxAmount()); // 8% VAT = 320,000
        assertEquals(new BigDecimal("4320000.00"), res.getGrandTotal()); // 4,320,000
        assertEquals(PurchaseOrderEntity.POStatus.PENDING_APPROVAL, res.getStatus());
    }

    @Test
    void testApprovePO_Success() {
        PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                .id(100L)
                .companyId(1L)
                .poNumber("PO-123")
                .status(PurchaseOrderEntity.POStatus.PENDING_APPROVAL)
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(po));
        when(companyUserRepository.findByCompanyIdAndUserIdAndIsActiveTrue(1L, 20L))
                .thenReturn(Optional.of(approverUser));
        when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseOrderItemRepository.findByPurchaseOrderId(100L)).thenReturn(Collections.emptyList());

        PurchaseOrderResponseDto res = purchaseOrderService.approvePO(100L, 20L);

        assertEquals(PurchaseOrderEntity.POStatus.APPROVED, res.getStatus());
        assertEquals(20L, res.getApprovedBy());
    }

    @Test
    void testRejectPO_Success() {
        PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                .id(100L)
                .companyId(1L)
                .status(PurchaseOrderEntity.POStatus.PENDING_APPROVAL)
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(po));
        when(companyUserRepository.findByCompanyIdAndUserIdAndIsActiveTrue(1L, 20L))
                .thenReturn(Optional.of(approverUser));
        when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseOrderItemRepository.findByPurchaseOrderId(100L)).thenReturn(Collections.emptyList());

        PurchaseOrderResponseDto res = purchaseOrderService.rejectPO(100L, 20L, "Vượt quá ngân sách quý");

        assertEquals(PurchaseOrderEntity.POStatus.REJECTED, res.getStatus());
        assertEquals("Vượt quá ngân sách quý", res.getRejectionReason());
    }

    @Test
    void testConvertToOrder_Success() {
        PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                .id(100L)
                .companyId(1L)
                .status(PurchaseOrderEntity.POStatus.APPROVED)
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(po));
        when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseOrderItemRepository.findByPurchaseOrderId(100L)).thenReturn(Collections.emptyList());

        PurchaseOrderResponseDto res = purchaseOrderService.convertToOrder(100L, 10L, 5001);

        assertEquals(PurchaseOrderEntity.POStatus.CONVERTED_TO_ORDER, res.getStatus());
        assertEquals(5001, res.getConvertedOrderId());
    }
}
