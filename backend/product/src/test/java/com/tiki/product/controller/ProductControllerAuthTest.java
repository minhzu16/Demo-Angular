package com.tiki.product.controller;

import com.tiki.product.client.ShopClient;
import com.tiki.product.dto.ProductDetailDTO;
import com.tiki.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerAuthTest {

    @Mock private ProductService productService;
    @Mock private ShopClient shopClient;
    @InjectMocks private ProductController controller;

    private static HttpStatus statusOf(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    @DisplayName("Sửa/xóa/tạo sản phẩm không có danh tính -> 401 (trước đây bỏ qua kiểm tra chủ sở hữu)")
    void writes_requireIdentity() {
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.update(1, new ProductDetailDTO(), null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.delete(1, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.create(new ProductDetailDTO(), null, null))));
        verify(productService, never()).update(any(), any(), any());
        verify(productService, never()).delete(any(), any());
    }

    @Test
    @DisplayName("Seller: ownership được kiểm tra theo userId; admin bỏ qua ownership")
    void seller_isOwnershipChecked_adminBypasses() {
        ProductDetailDTO dto = new ProductDetailDTO();
        controller.update(7, dto, 50L, "SELLER");
        verify(productService).update(7, dto, 50L);

        controller.delete(7, 1L, "ROLE_ADMIN");
        verify(productService).delete(7, null);
    }

    @Test
    @DisplayName("Tạo sản phẩm: shopId luôn là shop của seller, bỏ qua shopId trong body")
    void create_forcesOwnShop() {
        when(shopClient.getShopBySeller(50L)).thenReturn(new ShopClient.ShopInfo(5L, "My shop"));
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setShopId(999L); // claims another shop

        controller.create(dto, 50L, "SELLER");

        assertEquals(5L, dto.getShopId());
        verify(productService).create(dto, 50L);
    }

    @Test
    @DisplayName("Seller chưa có shop / không xác minh được shop -> 403, không tạo")
    void create_withoutVerifiableShop_isForbidden() {
        when(shopClient.getShopBySeller(50L)).thenReturn(null);
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.create(new ProductDetailDTO(), 50L, "SELLER"))));

        when(shopClient.getShopBySeller(51L)).thenThrow(new RuntimeException("down"));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(assertThrows(ResponseStatusException.class,
                () -> controller.create(new ProductDetailDTO(), 51L, "SELLER"))));
        verify(productService, never()).create(any(), any());
    }
}
