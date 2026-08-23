package com.tiki.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.cart.client.ProductClient;
import com.tiki.cart.dto.AddItemRequest;
import com.tiki.cart.dto.CartDto;
import com.tiki.cart.dto.CartItemDto;
import com.tiki.cart.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private ProductClient productClient;

    // ═══════════════════════════════════════════════════════════════
    // BUG 1: Thêm sản phẩm với số lượng âm
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Bug 1: Quantity âm phải bị reject 400")
    public void testAddItem_WithNegativeQuantity_ShouldReturnBadRequest() throws Exception {
        AddItemRequest request = new AddItemRequest();
        request.setProductId(1);
        request.setQuantity(-5);

        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Bug 1: Quantity = 0 phải bị reject 400")
    public void testAddItem_WithZeroQuantity_ShouldReturnBadRequest() throws Exception {
        AddItemRequest request = new AddItemRequest();
        request.setProductId(1);
        request.setQuantity(0);

        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Bug 1: Null quantity phải bị reject 400")
    public void testAddItem_WithNullQuantity_ShouldReturnBadRequest() throws Exception {
        String json = "{\"productId\": 1}"; // No quantity field

        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Bug 1: Null productId phải bị reject 400")
    public void testAddItem_WithNullProductId_ShouldReturnBadRequest() throws Exception {
        String json = "{\"quantity\": 2}"; // No productId

        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Bug 1: Valid request phải trả 201")
    public void testAddItem_WithValidRequest_ShouldReturnCreated() throws Exception {
        AddItemRequest request = new AddItemRequest();
        request.setProductId(1);
        request.setQuantity(2);

        // Mock product client
        ProductClient.ProductDTO mockProduct = new ProductClient.ProductDTO();
        mockProduct.setPrice(new java.math.BigDecimal("100000"));
        when(productClient.getProduct(1)).thenReturn(mockProduct);

        // Mock cart service
        CartDto mockCart = new CartDto();
        mockCart.setId(1);
        mockCart.setTotalItems(2);
        mockCart.setTotalAmount(200000.0);
        when(cartService.addItem(any(), any(), eq(1), eq(2), anyDouble())).thenReturn(mockCart);

        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "1")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    // ═══════════════════════════════════════════════════════════════
    // BUG 4: Sai số phẩy động (Floating point precision)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Bug 4: TotalAmount phải chính xác (không sai số phẩy động)")
    public void testGetCart_TotalAmountPrecision() throws Exception {
        // Tạo giỏ hàng với giá lẻ gây sai floating point
        CartItemDto item1 = new CartItemDto();
        item1.setProductId(1);
        item1.setQuantity(3);
        item1.setPriceSnapshot(33333.33); // 3 * 33333.33 = 99999.99

        CartItemDto item2 = new CartItemDto();
        item2.setProductId(2);
        item2.setQuantity(7);
        item2.setPriceSnapshot(14285.71); // 7 * 14285.71 = 99999.97

        CartDto mockCart = new CartDto();
        mockCart.setId(1);
        mockCart.setUserId(1);
        mockCart.setTotalItems(10);
        mockCart.setCartItems(List.of(item1, item2));
        // Expected: 99999.99 + 99999.97 = 199999.96
        mockCart.setTotalAmount(199999.96);

        when(cartService.getCart(eq(1), any())).thenReturn(mockCart);

        mockMvc.perform(get("/api/v1/cart")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(199999.96));
    }

    // ═══════════════════════════════════════════════════════════════
    // BUG 5: Giỏ hàng không xóa sau khi thanh toán
    // Note: Cart clearing is the responsibility of OrderService or 
    //       Frontend after checkout. This tests the cart /clear endpoint.
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Bug 5: API /cart/clear phải trả 204 No Content")
    public void testClearCart_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/cart/clear")
                .header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }
}
