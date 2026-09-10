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

        lenient().when(fraudDetectionService.assessOrderRisk(any())).thenReturn(
                new FraudDetectionService.FraudAssessment(0, "LOW", "Bình thường")
        );
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
}
