package com.tiki.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.entity.OrderTrackingEntity;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderTrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTrackingRepository orderTrackingRepository;

    @InjectMocks
    private OrderTrackingController orderTrackingController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderTrackingController).build();
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/tracking returns database timeline records")
    void testGetTrackingInfo() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(100);
        order.setStatus(OrderEntity.OrderStatus.SHIPPING);
        order.setCreatedAt(LocalDateTime.now().minusDays(1));

        OrderTrackingEntity event1 = new OrderTrackingEntity(100, OrderEntity.OrderStatus.CONFIRMED, "Người bán đã xác nhận");
        event1.setLocation("Kho Hà Nội");
        event1.setCreatedAt(LocalDateTime.now().minusHours(5));

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderTrackingRepository.findByOrderIdOrderByCreatedAtDesc(100)).thenReturn(List.of(event1));

        mockMvc.perform(get("/api/v1/orders/100/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("SHIPPING"))
                .andExpect(jsonPath("$.currentLocation").value("Kho Hà Nội"))
                .andExpect(jsonPath("$.trackingHistory[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.trackingHistory[0].location").value("Kho Hà Nội"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/delivery-status returns proper progress and flags")
    void testGetDeliveryStatus() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(100);
        order.setStatus(OrderEntity.OrderStatus.DELIVERED);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/orders/100/delivery-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.deliveryProgress").value(100))
                .andExpect(jsonPath("$.canReturn").value(true))
                .andExpect(jsonPath("$.canCancel").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/tracking records new milestone")
    void testAddTrackingMilestone() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(100);
        order.setStatus(OrderEntity.OrderStatus.SHIPPING);

        OrderTrackingController.AddTrackingEventRequest request = new OrderTrackingController.AddTrackingEventRequest();
        request.setLocation("Bưu cục Cầu Giấy");
        request.setNote("Đang xuất kho đi giao");
        request.setStatus(OrderEntity.OrderStatus.SHIPPING);

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(orderTrackingRepository.save(any(OrderTrackingEntity.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/v1/orders/100/tracking")
                        .header("X-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location").value("Bưu cục Cầu Giấy"))
                .andExpect(jsonPath("$.note").value("Đang xuất kho đi giao"));
    }
}
