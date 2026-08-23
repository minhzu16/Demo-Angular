package com.tiki.cart.service;

import com.tiki.cart.dto.CartDto;
import com.tiki.cart.entity.CartEntity;
import com.tiki.cart.entity.CartItemEntity;
import com.tiki.cart.repository.CartItemRepository;
import com.tiki.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(CartService.class)
@Transactional
public class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void testGetCart_CreatesNewIfNotExist() {
        CartDto cart = cartService.getCart(1, null);
        assertNotNull(cart);
        assertEquals(1, cart.getUserId());
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void testAddItem_SuccessfullyAddsToCart() {
        CartDto cart = cartService.addItem(1, null, 101, 2, 99.0);
        assertNotNull(cart);
        assertEquals(1, cart.getCartItems().size());
        assertEquals(101, cart.getCartItems().get(0).getProductId());
        assertEquals(2, cart.getCartItems().get(0).getQuantity());
        assertEquals(99.0, cart.getCartItems().get(0).getPriceSnapshot());
        assertEquals(198.0, cart.getTotalAmount());
    }

    @Test
    void testUpdateQty_SuccessfullyModifiesQuantity() {
        cartService.addItem(1, null, 101, 2, 99.0);
        CartDto updatedCart = cartService.updateQty(1, null, 101, 5);
        assertNotNull(updatedCart);
        assertEquals(1, updatedCart.getCartItems().size());
        assertEquals(5, updatedCart.getCartItems().get(0).getQuantity());
        assertEquals(495.0, updatedCart.getTotalAmount());
    }

    @Test
    void testRemoveItem_RemovesProduct() {
        cartService.addItem(1, null, 101, 2, 99.0);
        cartService.addItem(1, null, 102, 1, 50.0);
        
        CartDto updated = cartService.removeItem(1, null, 101);
        assertEquals(1, updated.getCartItems().size());
        assertEquals(102, updated.getCartItems().get(0).getProductId());
    }

    @Test
    void testClearCart_RemovesAllItems() {
        cartService.addItem(1, null, 101, 2, 99.0);
        cartService.clearCart(1, null);
        
        CartDto cart = cartService.getCart(1, null);
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void testMerge_CombinesSessionAndUserCarts() {
        // Create user cart with some items
        cartService.addItem(1, null, 101, 2, 99.0);

        // Create guest cart
        cartService.addItem(null, "session-abc", 101, 1, 99.0);
        cartService.addItem(null, "session-abc", 102, 3, 50.0);

        // Merge
        CartDto merged = cartService.merge(1, "session-abc");
        assertNotNull(merged);
        
        // Product 101 quantity should be 2 + 1 = 3
        // Product 102 quantity should be 3
        assertEquals(2, merged.getCartItems().size());
        
        int qty101 = merged.getCartItems().stream()
                .filter(i -> i.getProductId() == 101)
                .findFirst().map(i -> i.getQuantity()).orElse(0);
        int qty102 = merged.getCartItems().stream()
                .filter(i -> i.getProductId() == 102)
                .findFirst().map(i -> i.getQuantity()).orElse(0);
                
        assertEquals(3, qty101);
        assertEquals(3, qty102);
    }
}
