package com.tiki.order.service;

import com.tiki.order.client.UserClient;
import com.tiki.order.client.WarehouseClient;
import com.tiki.order.dto.CreateOrderRequest;
import com.tiki.order.dto.OrderDto;
import com.tiki.order.entity.OrderEntity;
import com.tiki.order.enums.PaymentMethod;
import com.tiki.order.enums.PaymentStatus;
import com.tiki.order.repository.OrderRepository;
import com.tiki.order.repository.OrderTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderCreationServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTrackingRepository orderTrackingRepository;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private VoucherService voucherService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private com.tiki.order.client.PaymentClient paymentClient;

    @Mock
    private com.tiki.order.client.ProductClient productClient;

    @Mock
    private com.tiki.order.saga.CheckoutSagaOrchestrator sagaOrchestrator;

    @Mock
    private com.tiki.order.repository.OrderIdempotencyRepository orderIdempotencyRepository;

    @Mock
    private com.tiki.order.repository.OutboxEventRepository outboxEventRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private OrderCreationService orderCreationService;

    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1);
        
        CreateOrderRequest.ShippingAddressDto address = new CreateOrderRequest.ShippingAddressDto();
        address.setFullName("John Doe");
        address.setPhoneNumber("0901234567");
        address.setProvince("HCM");
        address.setDistrict("District 1");
        address.setStreet("123 Le Loi");
        createOrderRequest.setShippingAddress(address);

        List<CreateOrderRequest.OrderItemDto> items = new ArrayList<>();
        CreateOrderRequest.OrderItemDto item = new CreateOrderRequest.OrderItemDto();
        item.setProductId(101);
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100000"));
        items.add(item);
        createOrderRequest.setItems(items);
        
        createOrderRequest.setPaymentMethod(PaymentMethod.COD);

        lenient().when(productClient.getBatchPricing(any())).thenReturn(List.of(
                com.tiki.order.dto.ProductPricingDto.builder()
                        .productId(101)
                        .name("Test Product")
                        .price(new BigDecimal("100000"))
                        .shopId(1L)
                        .thumbnailUrl("http://image.png")
                        .status("ACTIVE")
                        .stock(50)
                        .build()
        ));

        lenient().when(fraudDetectionService.assessOrderRisk(any())).thenReturn(
                new FraudDetectionService.FraudAssessment(0, "LOW", "Bình thường")
        );

        lenient().when(warehouseClient.reserveStock(any(), any())).thenReturn(true);
    }

    @Test
    void createOrder_Success_WithSmallAmount_AddsShippingFee() {
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(1);
            return order;
        });

        OrderDto mockDto = new OrderDto();
        mockDto.setSubtotal(new BigDecimal("200000"));
        mockDto.setShippingFee(new BigDecimal("30000"));
        mockDto.setTotalAmount(new BigDecimal("230000"));
        mockDto.setPaymentStatus(PaymentStatus.PENDING);
        
        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(mockDto);

        OrderDto result = orderCreationService.createOrder(createOrderRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("200000"), result.getSubtotal());
        assertEquals(new BigDecimal("30000"), result.getShippingFee());
        assertEquals(new BigDecimal("230000"), result.getTotalAmount());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        
        verify(userClient).getUser(1L);
        verify(warehouseClient).reserveStock(101L, 2);
        verify(rabbitTemplate).convertAndSend(eq("tiki.events"), eq("order.created"), any(Object.class));
    }

    @Test
    void createOrder_Success_WithLargeAmount_FreeShipping() {
        createOrderRequest.getItems().get(0).setQuantity(6);

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(1);
            return order;
        });

        OrderDto mockDto = new OrderDto();
        mockDto.setSubtotal(new BigDecimal("600000"));
        mockDto.setShippingFee(BigDecimal.ZERO);
        mockDto.setTotalAmount(new BigDecimal("600000"));

        when(orderMapper.toDto(any(OrderEntity.class))).thenReturn(mockDto);

        OrderDto result = orderCreationService.createOrder(createOrderRequest);

        assertEquals(new BigDecimal("600000"), result.getSubtotal());
        assertEquals(BigDecimal.ZERO, result.getShippingFee());
        assertEquals(new BigDecimal("600000"), result.getTotalAmount());
    }

    @Test
    void createOrder_NegativeTotal_Exploit() {
        // Mô phỏng hacker có 1 triệu điểm, dùng cho đơn hàng 200k
        com.tiki.common.dto.UserDto mockUser = new com.tiki.common.dto.UserDto();
        mockUser.setId(1L);
        mockUser.setLoyaltyPoints(1000000); // Có 1 triệu điểm
        when(userClient.getUser(1L)).thenReturn(mockUser);

        createOrderRequest.setUsePoints(1000000); // Dùng hết 1 triệu điểm

        // Lỗ hổng ĐÃ VÁ: Phải ném lỗi khi số điểm sử dụng vượt quá tổng giá trị đơn hàng
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderCreationService.createOrder(createOrderRequest);
        });

        assertEquals("Số điểm sử dụng không được vượt quá tổng giá trị đơn hàng.", exception.getMessage());
    }

    @Test
    void createOrder_SilentVoucherFailure_Exploit() {
        // Mô phỏng hacker dùng voucher hết hạn/lởm
        createOrderRequest.setVoucherCode("FAKE_VOUCHER");

        com.tiki.order.dto.VoucherValidationResponse mockResponse = new com.tiki.order.dto.VoucherValidationResponse();
        mockResponse.setValid(false); // Voucher không hợp lệ
        when(voucherService.validateVoucher(any())).thenReturn(mockResponse);

        // Lỗ hổng ĐÃ VÁ: Phải ném lỗi khi voucher không hợp lệ
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderCreationService.createOrder(createOrderRequest);
        });

        assertEquals("Voucher không hợp lệ hoặc đã hết hạn: FAKE_VOUCHER", exception.getMessage());
    }

    @Test
    void createOrder_NegativeQuantity_Exploit() {
        // Hacker cố tình truyền số lượng âm để giảm tổng tiền hóa đơn
        createOrderRequest.getItems().get(0).setQuantity(-10);

        // Lỗ hổng ĐÃ VÁ: Ném exception khi số lượng <= 0
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderCreationService.createOrder(createOrderRequest);
        });

        assertTrue(exception.getMessage().contains("không hợp lệ"));
    }

    @Test
    void createOrder_PriceTamperingExploit_IgnoredAndUsesServerPrice() {
        // Hacker cố tình can thiệp request để mua sản phẩm 100.000đ với giá 1đ
        createOrderRequest.getItems().get(0).setUnitPrice(new BigDecimal("1"));
        createOrderRequest.getItems().get(0).setPrice(new BigDecimal("1"));
        createOrderRequest.getItems().get(0).setQuantity(2);

        org.mockito.ArgumentCaptor<OrderEntity> orderCaptor = org.mockito.ArgumentCaptor.forClass(OrderEntity.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(2);
            return order;
        });

        when(orderMapper.toDto(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            OrderDto dto = new OrderDto();
            dto.setSubtotal(order.getSubtotal());
            dto.setTotalAmount(order.getTotalAmount());
            return dto;
        });

        OrderDto result = orderCreationService.createOrder(createOrderRequest);

        assertNotNull(result);
        OrderEntity savedOrder = orderCaptor.getValue();
        // Server phải lấy giá từ database (100.000đ * 2 = 200.000đ), không lấy giá 1đ do hacker gửi
        assertEquals(new BigDecimal("200000"), savedOrder.getSubtotal(), 
                "Lỗ hổng 3.2: Giá sản phẩm phải tính theo server database, không cho phép client quyết định!");
        assertEquals(new BigDecimal("100000"), savedOrder.getItems().get(0).getPrice());
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        // Hacker gửi productId không tồn tại trên hệ thống
        when(productClient.getBatchPricing(any())).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderCreationService.createOrder(createOrderRequest);
        });

        assertTrue(exception.getMessage().contains("không tồn tại"));
    }

    @Test
    void createOrder_IdempotencyCompleted_ReturnsCachedResponseImmediately() throws Exception {
        createOrderRequest.setIdempotencyKey("IDEMP_KEY_123");

        com.tiki.order.entity.OrderIdempotencyEntity completedRecord = com.tiki.order.entity.OrderIdempotencyEntity.builder()
                .idempotencyKey("IDEMP_KEY_123")
                .status("COMPLETED")
                .orderId(999)
                .responseBody("{\"id\":999,\"totalAmount\":230000}")
                .build();

        when(orderIdempotencyRepository.findByIdempotencyKey("IDEMP_KEY_123")).thenReturn(java.util.Optional.of(completedRecord));
        OrderDto cachedDto = new OrderDto();
        cachedDto.setId(999);
        cachedDto.setTotalAmount(new BigDecimal("230000"));
        when(objectMapper.readValue(completedRecord.getResponseBody(), OrderDto.class)).thenReturn(cachedDto);

        OrderDto result = orderCreationService.createOrder(createOrderRequest);

        assertNotNull(result);
        assertEquals(999, result.getId());
        verify(warehouseClient, never()).reserveStock(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_IdempotencyInProgress_ThrowsConflictException() {
        createOrderRequest.setIdempotencyKey("IDEMP_IN_PROGRESS");

        com.tiki.order.entity.OrderIdempotencyEntity inProgressRecord = com.tiki.order.entity.OrderIdempotencyEntity.builder()
                .idempotencyKey("IDEMP_IN_PROGRESS")
                .status("IN_PROGRESS")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(orderIdempotencyRepository.findByIdempotencyKey("IDEMP_IN_PROGRESS")).thenReturn(java.util.Optional.of(inProgressRecord));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderCreationService.createOrder(createOrderRequest);
        });

        assertTrue(exception.getMessage().contains("đang được xử lý"));
        verify(warehouseClient, never()).reserveStock(any(), any());
    }

    @Test
    void createOrder_StockReservationFails_TriggersSagaCompensation() {
        when(warehouseClient.reserveStock(any(), any())).thenThrow(new RuntimeException("Out of stock"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderCreationService.createOrder(createOrderRequest);
        });

        assertTrue(exception.getMessage().contains("không đủ số lượng"));
        verify(sagaOrchestrator).compensate(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_Success_SavesTransactionalOutboxEvent() {
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(100);
            return order;
        });

        when(orderMapper.toDto(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderDto dto = new OrderDto();
            dto.setId(100);
            return dto;
        });

        OrderDto result = orderCreationService.createOrder(createOrderRequest);

        assertNotNull(result);
        assertEquals(100, result.getId());
        // Verify Transactional Outbox event is saved
        verify(outboxEventRepository, atLeastOnce()).save(argThat(outbox -> 
                "ORDER".equals(outbox.getAggregateType()) &&
                Integer.valueOf(100).equals(outbox.getAggregateId()) &&
                "order.created".equals(outbox.getEventType())
        ));
    }
}
