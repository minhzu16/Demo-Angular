package com.tiki.order.service;

import com.tiki.common.dto.ComplaintDto;
import com.tiki.common.dto.CreateComplaintRequest;
import com.tiki.common.dto.ResolveComplaintRequest;
import com.tiki.common.entity.ComplaintEntity;
import com.tiki.common.repository.ComplaintRepository;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new OrderEntity();
        sampleOrder.setId(101);
        sampleOrder.setUserId(20L);
        sampleOrder.setStatus(OrderEntity.OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("createComplaint succeeds for order owned by buyer")
    void testCreateComplaintSuccess() {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .orderId(101L)
                .title("Sản phẩm bị lỗi")
                .description("Hàng nhận được bị vỡ màn hình")
                .build();

        when(orderRepository.findById(101)).thenReturn(Optional.of(sampleOrder));
        when(complaintRepository.existsByOrderIdAndStatus(101L, ComplaintEntity.Status.PENDING)).thenReturn(false);

        ComplaintEntity saved = new ComplaintEntity();
        saved.setId(1L);
        saved.setOrderId(101L);
        saved.setBuyerId(20L);
        saved.setTitle("Sản phẩm bị lỗi");
        saved.setStatus(ComplaintEntity.Status.PENDING);

        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(saved);

        ComplaintDto result = complaintService.createComplaint(20L, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Sản phẩm bị lỗi");
        assertThat(result.getStatus()).isEqualTo("PENDING");

        verify(complaintRepository).save(argThat(c -> 
                c.getOrderId().equals(101L) && 
                c.getBuyerId().equals(20L) && 
                c.getStatus() == ComplaintEntity.Status.PENDING));
    }

    @Test
    @DisplayName("createComplaint fails if buyer does not own the order (IDOR protection)")
    void testCreateComplaintUnauthorized() {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .orderId(101L)
                .title("Hacker complaint")
                .description("Trying to complaint someone else's order")
                .build();

        when(orderRepository.findById(101)).thenReturn(Optional.of(sampleOrder)); // order belongs to userId 20

        assertThatThrownBy(() -> complaintService.createComplaint(99L, request)) // userId 99 calls
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không có quyền");

        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("createComplaint fails if order is already cancelled")
    void testCreateComplaintCancelledOrder() {
        sampleOrder.setStatus(OrderEntity.OrderStatus.CANCELLED);

        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .orderId(101L)
                .title("Late complaint")
                .description("Order cancelled already")
                .build();

        when(orderRepository.findById(101)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> complaintService.createComplaint(20L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã bị hủy");
    }

    @Test
    @DisplayName("createComplaint fails if a pending complaint already exists for the order")
    void testCreateComplaintDuplicatePending() {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .orderId(101L)
                .title("Duplicate")
                .description("Second complaint")
                .build();

        when(orderRepository.findById(101)).thenReturn(Optional.of(sampleOrder));
        when(complaintRepository.existsByOrderIdAndStatus(101L, ComplaintEntity.Status.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> complaintService.createComplaint(20L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đang có một khiếu nại ở trạng thái chờ xử lý");
    }

    @Test
    @DisplayName("resolveComplaint updates complaint status to RESOLVED with resolution text")
    void testResolveComplaintSuccess() {
        ComplaintEntity existing = new ComplaintEntity();
        existing.setId(5L);
        existing.setOrderId(101L);
        existing.setStatus(ComplaintEntity.Status.PENDING);

        when(complaintRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenAnswer(i -> i.getArgument(0));

        ResolveComplaintRequest request = ResolveComplaintRequest.builder()
                .status(ComplaintEntity.Status.RESOLVED)
                .resolution("Đã đồng ý hoàn tiền cho khách hàng")
                .build();

        ComplaintDto result = complaintService.resolveComplaint(5L, 999L, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolution()).isEqualTo("Đã đồng ý hoàn tiền cho khách hàng");
        assertThat(existing.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("resolveComplaint fails if complaint is already resolved")
    void testResolveComplaintAlreadyProcessed() {
        ComplaintEntity existing = new ComplaintEntity();
        existing.setId(5L);
        existing.setStatus(ComplaintEntity.Status.RESOLVED);

        when(complaintRepository.findById(5L)).thenReturn(Optional.of(existing));

        ResolveComplaintRequest request = ResolveComplaintRequest.builder()
                .status(ComplaintEntity.Status.REJECTED)
                .resolution("Rejected")
                .build();

        assertThatThrownBy(() -> complaintService.resolveComplaint(5L, 999L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã được phân xử trước đó");
    }
}
