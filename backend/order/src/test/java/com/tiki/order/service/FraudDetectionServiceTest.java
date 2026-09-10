package com.tiki.order.service;

import com.tiki.order.entity.OrderEntity;
import com.tiki.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new OrderEntity();
        sampleOrder.setId(101);
        sampleOrder.setOrderNumber("ORD-TEST-001");
        sampleOrder.setUserId(20L);
        sampleOrder.setCustomerPhone("0987654321");
        sampleOrder.setShippingAddress("123 Đường Cầu Giấy, Hà Nội");
        sampleOrder.setTotalAmount(new BigDecimal("500000.00"));
    }

    @Test
    @DisplayName("assessOrderRisk - Normal order has LOW risk")
    void testNormalOrder_LowRisk() {
        when(orderRepository.countByUserIdAndCreatedAtAfter(eq(20L), any(LocalDateTime.class))).thenReturn(0L);
        when(orderRepository.countByCustomerPhoneAndCreatedAtAfter(eq("0987654321"), any(LocalDateTime.class))).thenReturn(0L);

        FraudDetectionService.FraudAssessment assessment = fraudDetectionService.assessOrderRisk(sampleOrder);

        assertThat(assessment.getRiskLevel()).isEqualTo("LOW");
        assertThat(assessment.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("assessOrderRisk - Rapid orders within 10 minutes flagged with score >= 40")
    void testRapidFireOrders_Flagged() {
        when(orderRepository.countByUserIdAndCreatedAtAfter(eq(20L), any(LocalDateTime.class))).thenReturn(3L);
        when(orderRepository.countByCustomerPhoneAndCreatedAtAfter(eq("0987654321"), any(LocalDateTime.class))).thenReturn(3L);

        FraudDetectionService.FraudAssessment assessment = fraudDetectionService.assessOrderRisk(sampleOrder);

        assertThat(assessment.getRiskLevel()).isEqualTo("HIGH");
        assertThat(assessment.getScore()).isGreaterThanOrEqualTo(70);
        assertThat(assessment.getReason()).contains("RAPID_FIRE_ORDERS");
    }

    @Test
    @DisplayName("assessOrderRisk - High value order (>20M) from new account flagged as MEDIUM risk")
    void testHighValueFirstOrder_MediumRisk() {
        sampleOrder.setTotalAmount(new BigDecimal("25000000.00"));

        when(orderRepository.countByUserIdAndCreatedAtAfter(eq(20L), any(LocalDateTime.class))).thenReturn(0L);
        when(orderRepository.countByCustomerPhoneAndCreatedAtAfter(eq("0987654321"), any(LocalDateTime.class))).thenReturn(0L);
        when(orderRepository.countByUserIdAndStatus(20L, OrderEntity.OrderStatus.DELIVERED)).thenReturn(0L);

        FraudDetectionService.FraudAssessment assessment = fraudDetectionService.assessOrderRisk(sampleOrder);

        assertThat(assessment.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(assessment.getScore()).isGreaterThanOrEqualTo(35);
        assertThat(assessment.getReason()).contains("HIGH_VALUE_FIRST_ORDER");
    }

    @Test
    @DisplayName("assessOrderRisk - Combined signals escalate to HIGH risk")
    void testCombinedSignals_HighRisk() {
        sampleOrder.setTotalAmount(new BigDecimal("30000000.00"));

        when(orderRepository.countByUserIdAndCreatedAtAfter(eq(20L), any(LocalDateTime.class))).thenReturn(2L);
        when(orderRepository.countByCustomerPhoneAndCreatedAtAfter(eq("0987654321"), any(LocalDateTime.class))).thenReturn(0L);
        when(orderRepository.countByUserIdAndStatus(20L, OrderEntity.OrderStatus.DELIVERED)).thenReturn(0L);

        FraudDetectionService.FraudAssessment assessment = fraudDetectionService.assessOrderRisk(sampleOrder);

        assertThat(assessment.getRiskLevel()).isEqualTo("HIGH");
        assertThat(assessment.getScore()).isGreaterThanOrEqualTo(70);
        assertThat(assessment.getReason()).contains("RAPID_FIRE_ORDERS");
        assertThat(assessment.getReason()).contains("HIGH_VALUE_FIRST_ORDER");
    }
}
