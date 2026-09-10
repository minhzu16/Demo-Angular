package com.tiki.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.payment.dto.PaymentDto;
import com.tiki.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("SePay Webhook: Từ chối 401 khi header API Key không khớp")
    void sepayWebhook_Unauthorized_WhenApiKeyMismatch() throws Exception {
        ReflectionTestUtils.setField(controller, "sepayWebhookSecret", "SECRET_KEY_123");

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "DH123");
        payload.put("transferAmount", 100000);

        mockMvc.perform(post("/api/v1/payments/sepay-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Apikey WRONG_KEY")
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SePay Webhook: Từ chối 400 Bad Request khi chuyển thiếu tiền (underpaid)")
    void sepayWebhook_BadRequest_WhenUnderpaid() throws Exception {
        PaymentDto mockPayment = PaymentDto.builder()
                .orderId(123)
                .amount(new BigDecimal("500000"))
                .paymentStatus("PENDING")
                .build();
        when(paymentService.getPaymentInfoByOrderId(123)).thenReturn(mockPayment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "DH123");
        payload.put("transferAmount", 100000); // Only paid 100k for 500k order

        mockMvc.perform(post("/api/v1/payments/sepay-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("underpaid"));

        verify(paymentService, never()).updatePaymentStatusByOrderId(eq(123), anyString());
    }

    @Test
    @DisplayName("SePay Webhook: Xử lý thành công và chuyển trạng thái đơn hàng khi thanh toán đủ")
    void sepayWebhook_Success_WhenAmountMatches() throws Exception {
        PaymentDto mockPayment = PaymentDto.builder()
                .orderId(123)
                .amount(new BigDecimal("500000"))
                .paymentStatus("PENDING")
                .build();
        when(paymentService.getPaymentInfoByOrderId(123)).thenReturn(mockPayment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Thanh toan don hang DH123");
        payload.put("transferAmount", 500000);

        mockMvc.perform(post("/api/v1/payments/sepay-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(paymentService).updatePaymentStatusByOrderId(123, "COMPLETED");
    }

    @Test
    @DisplayName("SePay Webhook: Idempotency - Bỏ qua xử lý trùng khi đơn hàng đã COMPLETED")
    void sepayWebhook_Idempotent_WhenAlreadyCompleted() throws Exception {
        PaymentDto mockPayment = PaymentDto.builder()
                .orderId(123)
                .amount(new BigDecimal("500000"))
                .paymentStatus("COMPLETED")
                .build();
        when(paymentService.getPaymentInfoByOrderId(123)).thenReturn(mockPayment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "DH123");
        payload.put("transferAmount", 500000);

        mockMvc.perform(post("/api/v1/payments/sepay-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("already_processed"));

        verify(paymentService, never()).updatePaymentStatusByOrderId(eq(123), anyString());
    }

    @Test
    @DisplayName("POST /order/{orderId}/refund returns 200 OK with REFUNDED status")
    void testRefundPayment() throws Exception {
        PaymentDto mockPayment = PaymentDto.builder()
                .orderId(123)
                .amount(new BigDecimal("500000"))
                .paymentStatus("REFUNDED")
                .build();
        when(paymentService.refundPayment(123)).thenReturn(mockPayment);

        mockMvc.perform(post("/api/v1/payments/order/123/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(123))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        verify(paymentService).refundPayment(123);
    }
}
