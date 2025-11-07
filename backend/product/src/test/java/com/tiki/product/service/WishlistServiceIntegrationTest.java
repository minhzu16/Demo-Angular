package com.tiki.product.service;

import com.tiki.common.entity.WishlistEntity;
import com.tiki.product.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WishlistService
 * Sprint 10 - Wishlist Feature
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WishlistServiceIntegrationTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private WishlistRepository wishlistRepository;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PRODUCT_ID = 101L;

    @BeforeEach
    void setUp() {
        // Clean up test data
        wishlistRepository.deleteAll();
    }

    @Test
    void testAddToWishlist_Success() {
        // When
        WishlistEntity wishlist = wishlistService.addToWishlist(
            TEST_USER_ID, 
            TEST_PRODUCT_ID, 
            null, 
            "Test note"
        );

        // Then
        assertNotNull(wishlist);
        assertNotNull(wishlist.getId());
        assertEquals(TEST_USER_ID, wishlist.getUserId());
        assertEquals(TEST_PRODUCT_ID, wishlist.getProductId());
        assertEquals("Test note", wishlist.getNotes());
        assertNotNull(wishlist.getAddedAt());
    }

    @Test
    void testAddToWishlist_Duplicate_ReturnsSame() {
        // Given
        WishlistEntity first = wishlistService.addToWishlist(
            TEST_USER_ID, 
            TEST_PRODUCT_ID, 
            null, 
            "First"
        );

        // When - Add same product again
        WishlistEntity second = wishlistService.addToWishlist(
            TEST_USER_ID, 
            TEST_PRODUCT_ID, 
            null, 
            "Second"
        );

        // Then - Should return existing
        assertEquals(first.getId(), second.getId());
    }

    @Test
    void testGetUserWishlist_Success() {
        // Given
        wishlistService.addToWishlist(TEST_USER_ID, 101L, null, "Product 1");
        wishlistService.addToWishlist(TEST_USER_ID, 102L, null, "Product 2");
        wishlistService.addToWishlist(TEST_USER_ID, 103L, null, "Product 3");

        // When
        Page<WishlistEntity> wishlist = wishlistService.getUserWishlist(
            TEST_USER_ID, 
            PageRequest.of(0, 10)
        );

        // Then
        assertNotNull(wishlist);
        assertEquals(3, wishlist.getTotalElements());
        assertEquals(1, wishlist.getTotalPages());
    }

    @Test
    void testIsInWishlist_True() {
        // Given
        wishlistService.addToWishlist(TEST_USER_ID, TEST_PRODUCT_ID, null, null);

        // When
        boolean inWishlist = wishlistService.isInWishlist(TEST_USER_ID, TEST_PRODUCT_ID);

        // Then
        assertTrue(inWishlist);
    }

    @Test
    void testIsInWishlist_False() {
        // When
        boolean inWishlist = wishlistService.isInWishlist(TEST_USER_ID, 999L);

        // Then
        assertFalse(inWishlist);
    }

    @Test
    void testGetWishlistCount_Success() {
        // Given
        wishlistService.addToWishlist(TEST_USER_ID, 101L, null, null);
        wishlistService.addToWishlist(TEST_USER_ID, 102L, null, null);

        // When
        Long count = wishlistService.getWishlistCount(TEST_USER_ID);

        // Then
        assertEquals(2L, count);
    }

    @Test
    void testRemoveFromWishlist_Success() {
        // Given
        wishlistService.addToWishlist(TEST_USER_ID, TEST_PRODUCT_ID, null, null);
        assertTrue(wishlistService.isInWishlist(TEST_USER_ID, TEST_PRODUCT_ID));

        // When
        wishlistService.removeFromWishlist(TEST_USER_ID, TEST_PRODUCT_ID);

        // Then
        assertFalse(wishlistService.isInWishlist(TEST_USER_ID, TEST_PRODUCT_ID));
    }

    @Test
    void testClearWishlist_Success() {
        // Given
        wishlistService.addToWishlist(TEST_USER_ID, 101L, null, null);
        wishlistService.addToWishlist(TEST_USER_ID, 102L, null, null);
        wishlistService.addToWishlist(TEST_USER_ID, 103L, null, null);
        assertEquals(3L, wishlistService.getWishlistCount(TEST_USER_ID));

        // When
        wishlistService.clearWishlist(TEST_USER_ID);

        // Then
        assertEquals(0L, wishlistService.getWishlistCount(TEST_USER_ID));
    }

    @Test
    void testAddMultipleToWishlist_Success() {
        // Given
        List<Long> productIds = List.of(101L, 102L, 103L);

        // When
        List<WishlistEntity> wishlists = wishlistService.addMultipleToWishlist(
            TEST_USER_ID, 
            productIds
        );

        // Then
        assertEquals(3, wishlists.size());
        assertEquals(3L, wishlistService.getWishlistCount(TEST_USER_ID));
    }

    @Test
    void testGetWishlistProductIds_Success() {
        // Given
        wishlistService.addToWishlist(TEST_USER_ID, 101L, null, null);
        wishlistService.addToWishlist(TEST_USER_ID, 102L, null, null);
        wishlistService.addToWishlist(TEST_USER_ID, 103L, null, null);

        // When
        List<Long> productIds = wishlistService.getWishlistProductIds(TEST_USER_ID);

        // Then
        assertEquals(3, productIds.size());
        assertTrue(productIds.contains(101L));
        assertTrue(productIds.contains(102L));
        assertTrue(productIds.contains(103L));
    }

    @Test
    void testWishlistPagination_Success() {
        // Given - Add 25 products
        for (long i = 1; i <= 25; i++) {
            wishlistService.addToWishlist(TEST_USER_ID, i, null, null);
        }

        // When - Get first page (10 items)
        Page<WishlistEntity> page1 = wishlistService.getUserWishlist(
            TEST_USER_ID, 
            PageRequest.of(0, 10)
        );

        // Then
        assertEquals(25, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertEquals(10, page1.getContent().size());

        // When - Get second page
        Page<WishlistEntity> page2 = wishlistService.getUserWishlist(
            TEST_USER_ID, 
            PageRequest.of(1, 10)
        );

        // Then
        assertEquals(10, page2.getContent().size());
    }
}
