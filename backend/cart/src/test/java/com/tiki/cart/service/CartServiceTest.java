package com.tiki.cart.service;

import com.tiki.cart.dto.CartDto;
import com.tiki.cart.entity.CartEntity;
import com.tiki.cart.entity.CartItemEntity;
import com.tiki.cart.repository.CartItemRepository;
import com.tiki.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepo;

    @Mock
    private CartItemRepository itemRepo;

    @InjectMocks
    private CartService cartService;

    @Test
    public void testGetCart_ExistingUserCart() {
        CartEntity entity = new CartEntity();
        entity.setId(1);
        entity.setUserId(42);
        entity.setIsActive(true);

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(entity));

        CartDto dto = cartService.getCart(42, null);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals(42, dto.getUserId());
        verify(cartRepo, never()).save(any());
    }

    @Test
    public void testGetCart_NewUserCart() {
        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.empty());
        when(cartRepo.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto dto = cartService.getCart(42, null);

        assertNotNull(dto);
        assertEquals(42, dto.getUserId());
        verify(cartRepo).save(any(CartEntity.class));
    }

    @Test
    public void testAddItem_NewItem() {
        CartEntity entity = new CartEntity();
        entity.setId(1);
        entity.setUserId(42);
        entity.setItems(new ArrayList<>());

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(entity));
        when(cartRepo.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto dto = cartService.addItem(42, null, 101, 2, 15000.0);

        assertNotNull(dto);
        assertEquals(1, dto.getCartItems().size());
        assertEquals(101, dto.getCartItems().get(0).getProductId());
        assertEquals(2, dto.getCartItems().get(0).getQuantity());
        assertEquals(15000.0, dto.getCartItems().get(0).getPriceSnapshot());
        verify(cartRepo).save(entity);
    }

    @Test
    public void testAddItem_ExistingItem() {
        CartEntity entity = new CartEntity();
        entity.setId(1);
        entity.setUserId(42);
        
        CartItemEntity item = new CartItemEntity();
        item.setCart(entity);
        item.setProductId(101);
        item.setQuantity(2);
        item.setPriceSnapshot(15000.0);
        entity.getItems().add(item);

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(entity));
        when(cartRepo.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto dto = cartService.addItem(42, null, 101, 3, 15000.0);

        assertNotNull(dto);
        assertEquals(1, dto.getCartItems().size());
        assertEquals(5, dto.getCartItems().get(0).getQuantity());
        verify(cartRepo).save(entity);
    }

    @Test
    public void testUpdateQty() {
        CartEntity entity = new CartEntity();
        entity.setId(1);
        entity.setUserId(42);

        CartItemEntity item = new CartItemEntity();
        item.setCart(entity);
        item.setProductId(101);
        item.setQuantity(2);
        item.setPriceSnapshot(15000.0);
        entity.getItems().add(item);

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(entity));
        when(cartRepo.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto dto = cartService.updateQty(42, null, 101, 10);

        assertNotNull(dto);
        assertEquals(10, dto.getCartItems().get(0).getQuantity());
        verify(cartRepo).save(entity);
    }

    @Test
    public void testRemoveItem() {
        CartEntity entity = new CartEntity();
        entity.setId(1);
        entity.setUserId(42);

        CartItemEntity item = new CartItemEntity();
        item.setCart(entity);
        item.setProductId(101);
        item.setQuantity(2);
        entity.getItems().add(item);

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(entity));
        when(cartRepo.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartDto dto = cartService.removeItem(42, null, 101);

        assertNotNull(dto);
        assertEquals(0, dto.getCartItems().size());
        verify(cartRepo).save(entity);
    }

    @Test
    public void testClearCart() {
        CartEntity entity = new CartEntity();
        entity.setId(1);
        entity.setUserId(42);

        CartItemEntity item = new CartItemEntity();
        item.setCart(entity);
        item.setProductId(101);
        item.setQuantity(2);
        entity.getItems().add(item);

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(entity));

        cartService.clearCart(42, null);

        assertTrue(entity.getItems().isEmpty());
        verify(cartRepo).save(entity);
    }

    @Test
    public void testMerge() {
        CartEntity userCart = new CartEntity();
        userCart.setId(1);
        userCart.setUserId(42);

        CartEntity guestCart = new CartEntity();
        guestCart.setId(2);
        guestCart.setSessionId("guest_session");
        
        CartItemEntity item = new CartItemEntity();
        item.setCart(guestCart);
        item.setProductId(101);
        item.setQuantity(2);
        item.setPriceSnapshot(10.0);
        guestCart.getItems().add(item);

        when(cartRepo.findByUserIdAndIsActive(42, true)).thenReturn(Optional.of(userCart));
        when(cartRepo.findBySessionIdAndIsActive("guest_session", true)).thenReturn(Optional.of(guestCart));

        CartDto dto = cartService.merge(42, "guest_session");

        assertNotNull(dto);
        assertFalse(guestCart.getIsActive());
        verify(cartRepo).save(guestCart);
    }
}
