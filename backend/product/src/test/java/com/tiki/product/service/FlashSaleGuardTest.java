package com.tiki.product.service;

import com.tiki.common.event.OrderCreatedEvent;
import com.tiki.common.event.OrderItemDto;
import com.tiki.product.controller.FlashSaleController;
import com.tiki.product.entity.FlashSale;
import com.tiki.product.entity.FlashSaleProduct;
import com.tiki.product.listener.FlashSaleEventListener;
import com.tiki.product.repository.FlashSaleProductRepository;
import com.tiki.product.repository.FlashSaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashSaleGuardTest {

    @Mock
    private FlashSaleRepository flashSaleRepository;

    @Mock
    private FlashSaleProductRepository flashSaleProductRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FlashSaleController flashSaleController;

    @InjectMocks
    private FlashSaleEventListener flashSaleEventListener;

    private FlashSale activeSale;
    private FlashSaleProduct flashSaleProduct;

    @BeforeEach
    void setUp() {
        activeSale = new FlashSale();
        activeSale.setId(1L);
        activeSale.setName("Giờ Vàng Siêu Sale");
        activeSale.setStatus(FlashSale.FlashSaleStatus.ACTIVE);
        activeSale.setStartTime(LocalDateTime.now().minusHours(1));
        activeSale.setEndTime(LocalDateTime.now().plusHours(1));

        flashSaleProduct = new FlashSaleProduct();
        flashSaleProduct.setId(10L);
        flashSaleProduct.setFlashSale(activeSale);
        flashSaleProduct.setProductId(500L);
        flashSaleProduct.setOriginalPrice(BigDecimal.valueOf(1000000));
        flashSaleProduct.setSalePrice(BigDecimal.valueOf(500000));
        flashSaleProduct.setQuantityLimit(10);
        flashSaleProduct.setQuantitySold(8);
        flashSaleProduct.setMaxPerUser(2); // Max 2 per user
    }

    @Test
    @DisplayName("Flash Sale Validation - Thành công khi mua trong giới hạn và còn hàng")
    void testValidateItem_Success() {
        when(flashSaleRepository.findActiveFlashSales(any(LocalDateTime.class))).thenReturn(List.of(activeSale));
        when(flashSaleProductRepository.findByFlashSaleIdAndProductId(1L, 500L)).thenReturn(Optional.of(flashSaleProduct));

        ResponseEntity<?> response = flashSaleController.validateFlashSaleItem(500L, 2, 101L);

        assertNotNull(response);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("valid"));
        assertEquals(BigDecimal.valueOf(500000), body.get("salePrice"));
        assertEquals(2, body.get("remainingQuantity"));
    }

    @Test
    @DisplayName("Flash Sale Validation - Chặn mua khi số lượng vượt quá maxPerUser")
    void testValidateItem_ExceedsMaxPerUser() {
        when(flashSaleRepository.findActiveFlashSales(any(LocalDateTime.class))).thenReturn(List.of(activeSale));
        when(flashSaleProductRepository.findByFlashSaleIdAndProductId(1L, 500L)).thenReturn(Optional.of(flashSaleProduct));

        // Request 3 items while maxPerUser is 2
        ResponseEntity<?> response = flashSaleController.validateFlashSaleItem(500L, 3, 101L);

        assertNotNull(response);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("valid"));
        assertTrue(body.get("message").toString().contains("Mỗi khách hàng chỉ được mua tối đa 2"));
    }

    @Test
    @DisplayName("Flash Sale Validation - Chặn mua khi sản phẩm đã bán hết số lượng ưu đãi")
    void testValidateItem_SoldOut() {
        flashSaleProduct.setQuantitySold(10); // Đã bán hết 10/10

        when(flashSaleRepository.findActiveFlashSales(any(LocalDateTime.class))).thenReturn(List.of(activeSale));
        when(flashSaleProductRepository.findByFlashSaleIdAndProductId(1L, 500L)).thenReturn(Optional.of(flashSaleProduct));

        ResponseEntity<?> response = flashSaleController.validateFlashSaleItem(500L, 1, 101L);

        assertNotNull(response);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("valid"));
        assertTrue(body.get("message").toString().contains("đã bán hết số lượng ưu đãi"));
    }

    @Test
    @DisplayName("Flash Sale Event Listener - Chặn bán quá giới hạn số lượng (Clamping to quantityLimit)")
    void testEventListener_ClampsSoldQuantity() {
        when(flashSaleRepository.findActiveFlashSales(any(LocalDateTime.class))).thenReturn(List.of(activeSale));
        when(flashSaleProductRepository.findByFlashSaleId(1L)).thenReturn(List.of(flashSaleProduct));
        when(flashSaleProductRepository.save(any(FlashSaleProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        // Order contains quantity = 5, but only 2 items left until limit 10
        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(500L)
                .quantity(5)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(999)
                .items(List.of(itemDto))
                .build();

        flashSaleEventListener.handleOrderCreated(event);

        // quantitySold should be clamped at 10, not 13
        assertEquals(10, flashSaleProduct.getQuantitySold());
        verify(flashSaleProductRepository, atLeastOnce()).save(flashSaleProduct);
    }
}
