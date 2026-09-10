package com.tiki.order.service;

import com.tiki.common.dto.ComplaintDto;
import com.tiki.common.dto.CreateComplaintRequest;
import com.tiki.common.dto.ResolveComplaintRequest;
import com.tiki.common.dto.SellerComplaintResponseRequest;
import com.tiki.common.entity.ComplaintEntity;
import com.tiki.common.repository.ComplaintRepository;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;

    @Transactional
    public ComplaintDto createComplaint(Long buyerId, CreateComplaintRequest request) {
        log.info("Creating complaint: buyerId={}, orderId={}", buyerId, request.getOrderId());

        OrderEntity order = orderRepository.findById(request.getOrderId().intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        if (!order.getUserId().equals(buyerId)) {
            log.warn("Unauthorized complaint creation attempt: buyerId={}, orderUserId={}", buyerId, order.getUserId());
            throw new IllegalStateException("Bạn không có quyền tạo khiếu nại cho đơn hàng của người khác.");
        }

        if (order.getStatus() == OrderEntity.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Không thể tạo khiếu nại cho đơn hàng đã bị hủy.");
        }

        if (complaintRepository.existsByOrderIdAndStatus(request.getOrderId(), ComplaintEntity.Status.PENDING)) {
            throw new IllegalStateException("Đơn hàng này hiện đang có một khiếu nại ở trạng thái chờ xử lý.");
        }

        ComplaintEntity complaint = new ComplaintEntity();
        complaint.setOrderId(request.getOrderId());
        complaint.setBuyerId(buyerId);
        if (order.getShopId() != null) {
            complaint.setSellerId(order.getShopId());
        }
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setStatus(ComplaintEntity.Status.PENDING);
        complaint.setCreatedAt(LocalDateTime.now());

        ComplaintEntity saved = complaintRepository.save(complaint);
        log.info("Complaint created successfully: id={}, orderId={}, sellerId={}", saved.getId(), saved.getOrderId(), saved.getSellerId());
        return toDto(saved);
    }

    @Transactional
    public ComplaintDto submitSellerResponse(Long complaintId, Long sellerId, SellerComplaintResponseRequest request) {
        log.info("Seller {} submitting response for complaint {}", sellerId, complaintId);

        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại với ID: " + complaintId));

        if (complaint.getStatus() != ComplaintEntity.Status.PENDING) {
            throw new IllegalStateException("Khiếu nại này không ở trạng thái chờ phản hồi.");
        }

        if (complaint.getSellerId() != null && !complaint.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("Bạn không có quyền phản hồi khiếu nại của shop khác.");
        }

        complaint.setSellerResponse(request.getResponse());
        complaint.setSellerResponseAt(LocalDateTime.now());

        ComplaintEntity saved = complaintRepository.save(complaint);
        log.info("Seller response recorded for complaint id={}", saved.getId());
        return toDto(saved);
    }

    public List<ComplaintDto> getMyComplaints(Long buyerId) {
        return complaintRepository.findByBuyerId(buyerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ComplaintDto> getSellerComplaints(Long sellerId) {
        return complaintRepository.findBySellerId(sellerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ComplaintDto> getComplaintsByOrderId(Long orderId, Long requesterId, boolean isAdmin) {
        OrderEntity order = orderRepository.findById(orderId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

        if (!isAdmin && !order.getUserId().equals(requesterId)) {
            throw new IllegalStateException("Bạn không có quyền xem khiếu nại của đơn hàng này.");
        }

        return complaintRepository.findByOrderId(orderId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<ComplaintDto> getAdminComplaints(ComplaintEntity.Status status, Pageable pageable) {
        Page<ComplaintEntity> page = status != null 
                ? complaintRepository.findByStatus(status, pageable)
                : complaintRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    @Transactional
    public ComplaintDto resolveComplaint(Long complaintId, Long resolverId, ResolveComplaintRequest request) {
        log.info("Resolving complaint: id={}, resolverId={}, status={}", complaintId, resolverId, request.getStatus());

        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại với ID: " + complaintId));

        if (complaint.getStatus() != ComplaintEntity.Status.PENDING) {
            throw new IllegalStateException("Khiếu nại này đã được phân xử trước đó (trạng thái: " + complaint.getStatus() + ")");
        }

        complaint.setStatus(request.getStatus());
        complaint.setResolution(request.getResolution());
        complaint.setResolvedAt(LocalDateTime.now());

        String resType = request.getResolutionType();
        if (resType == null || resType.isBlank()) {
            resType = request.getStatus() == ComplaintEntity.Status.RESOLVED ? "RESOLVED" : "REJECTED";
        }
        complaint.setResolutionType(resType);

        // ✅ Q4: Tự động kích hoạt hoàn tiền đơn hàng nếu Admin phân xử chấp thuận hoàn tiền
        if (request.getStatus() == ComplaintEntity.Status.RESOLVED && 
                ("REFUND".equalsIgnoreCase(resType) || (request.getResolution() != null && request.getResolution().toLowerCase().contains("hoàn tiền")))) {
            try {
                log.info("Complaint arbitration triggered auto-refund for order {}", complaint.getOrderId());
                orderStatusService.refundOrder(complaint.getOrderId().intValue());
            } catch (Exception e) {
                log.error("Failed to auto-refund order {} during complaint resolution: {}", complaint.getOrderId(), e.getMessage());
            }
        }

        ComplaintEntity updated = complaintRepository.save(complaint);
        log.info("Complaint resolved successfully: id={}, finalStatus={}, type={}", updated.getId(), updated.getStatus(), resType);
        return toDto(updated);
    }

    private ComplaintDto toDto(ComplaintEntity entity) {
        ComplaintDto dto = new ComplaintDto();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setBuyerId(entity.getBuyerId());
        dto.setSellerId(entity.getSellerId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setResolution(entity.getResolution());
        dto.setSellerResponse(entity.getSellerResponse());
        dto.setSellerResponseAt(entity.getSellerResponseAt());
        dto.setResolutionType(entity.getResolutionType());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setResolvedAt(entity.getResolvedAt());
        return dto;
    }
}
