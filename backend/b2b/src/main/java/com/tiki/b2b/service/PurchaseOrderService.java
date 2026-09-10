package com.tiki.b2b.service;

import com.tiki.b2b.dto.PurchaseOrderCreateRequest;
import com.tiki.b2b.dto.PurchaseOrderItemDto;
import com.tiki.b2b.dto.PurchaseOrderResponseDto;
import com.tiki.b2b.entity.CompanyEntity;
import com.tiki.b2b.entity.CompanyUserEntity;
import com.tiki.b2b.entity.PurchaseOrderEntity;
import com.tiki.b2b.entity.PurchaseOrderItemEntity;
import com.tiki.b2b.repository.CompanyRepository;
import com.tiki.b2b.repository.CompanyUserRepository;
import com.tiki.b2b.repository.PurchaseOrderItemRepository;
import com.tiki.b2b.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final B2BService b2bService;

    @Transactional
    public PurchaseOrderResponseDto createPO(Long userId, PurchaseOrderCreateRequest req) {
        CompanyEntity company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy doanh nghiệp id=" + req.getCompanyId()));

        if (company.getStatus() != CompanyEntity.CompanyStatus.ACTIVE) {
            throw new IllegalStateException("Doanh nghiệp chưa được xác minh hoặc đang bị tạm khóa.");
        }

        CompanyUserEntity userMembership = companyUserRepository
                .findByCompanyIdAndUserIdAndIsActiveTrue(req.getCompanyId(), userId)
                .orElseThrow(() -> new SecurityException("Bạn không thuộc doanh nghiệp này hoặc tài khoản đã bị khóa."));

        if (userMembership.getRole() != CompanyUserEntity.CompanyRole.ADMIN &&
            userMembership.getRole() != CompanyUserEntity.CompanyRole.BUYER) {
            throw new SecurityException("Chỉ tài khoản ADMIN hoặc BUYER mới có quyền lập Purchase Order.");
        }

        String poNumber = "PO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PurchaseOrderItemEntity> items = new ArrayList<>();

        for (PurchaseOrderItemDto itemDto : req.getItems()) {
            BigDecimal unitPrice = b2bService.getB2BPrice(
                    itemDto.getProductId(),
                    itemDto.getQuantity(),
                    itemDto.getDefaultPrice() != null ? itemDto.getDefaultPrice() : BigDecimal.ZERO
            );

            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            items.add(PurchaseOrderItemEntity.builder()
                    .productId(itemDto.getProductId())
                    .productName(itemDto.getProductName())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(itemTotal)
                    .build());
        }

        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(taxAmount);

        PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                .poNumber(poNumber)
                .companyId(req.getCompanyId())
                .requestedBy(userId)
                .status(PurchaseOrderEntity.POStatus.PENDING_APPROVAL)
                .totalAmount(subtotal)
                .taxAmount(taxAmount)
                .grandTotal(grandTotal)
                .invoiceRequired(req.getInvoiceRequired() != null ? req.getInvoiceRequired() : true)
                .deliveryDate(req.getDeliveryDate())
                .note(req.getNote())
                .build();

        PurchaseOrderEntity savedPo = purchaseOrderRepository.save(po);

        items.forEach(i -> i.setPurchaseOrderId(savedPo.getId()));
        List<PurchaseOrderItemEntity> savedItems = purchaseOrderItemRepository.saveAll(items);

        log.info("Created PO {} for company {}, grandTotal={}", poNumber, req.getCompanyId(), grandTotal);
        return toDto(savedPo, savedItems);
    }

    @Transactional
    public PurchaseOrderResponseDto approvePO(Long poId, Long approverId) {
        PurchaseOrderEntity po = getPoEntity(poId);
        if (po.getStatus() != PurchaseOrderEntity.POStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Đơn mua hàng không ở trạng thái chờ duyệt (PENDING_APPROVAL).");
        }

        CompanyUserEntity approver = companyUserRepository
                .findByCompanyIdAndUserIdAndIsActiveTrue(po.getCompanyId(), approverId)
                .orElseThrow(() -> new SecurityException("Bạn không thuộc doanh nghiệp này."));

        if (approver.getRole() != CompanyUserEntity.CompanyRole.ADMIN &&
            approver.getRole() != CompanyUserEntity.CompanyRole.APPROVER) {
            throw new SecurityException("Chỉ tài khoản APPROVER hoặc ADMIN mới có quyền duyệt đơn mua hàng.");
        }

        po.setStatus(PurchaseOrderEntity.POStatus.APPROVED);
        po.setApprovedBy(approverId);
        PurchaseOrderEntity saved = purchaseOrderRepository.save(po);

        log.info("PO {} approved by userId={}", po.getPoNumber(), approverId);
        return toDto(saved, purchaseOrderItemRepository.findByPurchaseOrderId(poId));
    }

    @Transactional
    public PurchaseOrderResponseDto rejectPO(Long poId, Long approverId, String reason) {
        PurchaseOrderEntity po = getPoEntity(poId);
        if (po.getStatus() != PurchaseOrderEntity.POStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Đơn mua hàng không ở trạng thái chờ duyệt.");
        }

        CompanyUserEntity approver = companyUserRepository
                .findByCompanyIdAndUserIdAndIsActiveTrue(po.getCompanyId(), approverId)
                .orElseThrow(() -> new SecurityException("Bạn không thuộc doanh nghiệp này."));

        po.setStatus(PurchaseOrderEntity.POStatus.REJECTED);
        po.setRejectionReason(reason);
        PurchaseOrderEntity saved = purchaseOrderRepository.save(po);

        log.info("PO {} rejected by userId={}, reason={}", po.getPoNumber(), approverId, reason);
        return toDto(saved, purchaseOrderItemRepository.findByPurchaseOrderId(poId));
    }

    @Transactional
    public PurchaseOrderResponseDto convertToOrder(Long poId, Long userId, Integer orderId) {
        PurchaseOrderEntity po = getPoEntity(poId);
        if (po.getStatus() != PurchaseOrderEntity.POStatus.APPROVED) {
            throw new IllegalStateException("Chỉ đơn mua hàng đã được duyệt (APPROVED) mới có thể chuyển đổi thành đơn hàng.");
        }

        po.setStatus(PurchaseOrderEntity.POStatus.CONVERTED_TO_ORDER);
        po.setConvertedOrderId(orderId);
        PurchaseOrderEntity saved = purchaseOrderRepository.save(po);

        log.info("PO {} converted to order #{}", po.getPoNumber(), orderId);
        return toDto(saved, purchaseOrderItemRepository.findByPurchaseOrderId(poId));
    }

    public PurchaseOrderResponseDto getPOById(Long poId) {
        PurchaseOrderEntity po = getPoEntity(poId);
        return toDto(po, purchaseOrderItemRepository.findByPurchaseOrderId(poId));
    }

    public Page<PurchaseOrderResponseDto> getCompanyPOs(Long companyId, Pageable pageable) {
        return purchaseOrderRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(po -> toDto(po, purchaseOrderItemRepository.findByPurchaseOrderId(po.getId())));
    }

    private PurchaseOrderEntity getPoEntity(Long poId) {
        return purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mua hàng id=" + poId));
    }

    private PurchaseOrderResponseDto toDto(PurchaseOrderEntity po, List<PurchaseOrderItemEntity> items) {
        List<PurchaseOrderItemDto> itemDtos = items.stream().map(i -> PurchaseOrderItemDto.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .totalPrice(i.getTotalPrice())
                .build()
        ).collect(Collectors.toList());

        return PurchaseOrderResponseDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .companyId(po.getCompanyId())
                .requestedBy(po.getRequestedBy())
                .approvedBy(po.getApprovedBy())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .taxAmount(po.getTaxAmount())
                .grandTotal(po.getGrandTotal())
                .invoiceRequired(po.getInvoiceRequired())
                .deliveryDate(po.getDeliveryDate())
                .note(po.getNote())
                .rejectionReason(po.getRejectionReason())
                .convertedOrderId(po.getConvertedOrderId())
                .createdAt(po.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}
