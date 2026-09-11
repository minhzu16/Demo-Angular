package com.tiki.product.service;

import com.tiki.product.dto.CompareDto;
import com.tiki.product.entity.CompareEntity;
import com.tiki.product.entity.ProductEntity;
import com.tiki.product.repository.CompareRepository;
import com.tiki.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompareServiceTest {

    @Mock
    private CompareRepository compareRepo;

    @Mock
    private ProductRepository productRepo;

    @InjectMocks
    private CompareService compareService;

    @Test
    void testAddToCompare_Success() {
        ProductEntity product = new ProductEntity();
        product.setId(10);
        product.setName("iPhone 15 Pro");
        product.setPrice(new BigDecimal("28000000.00"));
        product.setBrand("Apple");

        when(productRepo.findById(10)).thenReturn(Optional.of(product));
        when(compareRepo.findByUserIdAndProductId(1L, 10)).thenReturn(Optional.empty());
        when(compareRepo.countByUserId(1L)).thenReturn(0L);

        CompareEntity saved = CompareEntity.builder()
                .id(100L)
                .userId(1L)
                .productId(10)
                .createdAt(LocalDateTime.now())
                .build();
        when(compareRepo.save(any(CompareEntity.class))).thenReturn(saved);

        CompareDto result = compareService.addToCompare(1L, 10);

        assertNotNull(result);
        assertEquals(10, result.getProductId());
        assertEquals("iPhone 15 Pro", result.getProductName());
        assertEquals(new BigDecimal("28000000.00"), result.getPrice());
        verify(compareRepo, times(1)).save(any(CompareEntity.class));
    }

    @Test
    void testAddToCompare_AlreadyExists_ReturnsExisting() {
        ProductEntity product = new ProductEntity();
        product.setId(10);
        product.setName("iPhone 15 Pro");
        product.setPrice(new BigDecimal("28000000.00"));

        CompareEntity existing = CompareEntity.builder()
                .id(50L)
                .userId(1L)
                .productId(10)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepo.findById(10)).thenReturn(Optional.of(product));
        when(compareRepo.findByUserIdAndProductId(1L, 10)).thenReturn(Optional.of(existing));

        CompareDto result = compareService.addToCompare(1L, 10);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        verify(compareRepo, never()).save(any());
    }

    @Test
    void testAddToCompare_ExceedsLimit_ThrowsException() {
        ProductEntity product = new ProductEntity();
        product.setId(55);

        when(productRepo.findById(55)).thenReturn(Optional.of(product));
        when(compareRepo.findByUserIdAndProductId(1L, 55)).thenReturn(Optional.empty());
        when(compareRepo.countByUserId(1L)).thenReturn(4L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compareService.addToCompare(1L, 55);
        });

        assertTrue(ex.getMessage().contains("tối đa 4 sản phẩm"));
        verify(compareRepo, never()).save(any());
    }

    @Test
    void testAddToCompare_ProductNotFound_ThrowsException() {
        when(productRepo.findById(999)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            compareService.addToCompare(1L, 999);
        });

        assertTrue(ex.getMessage().contains("Sản phẩm không tồn tại"));
    }

    @Test
    void testGetCompareList_EnrichesProductDetails() {
        CompareEntity c1 = CompareEntity.builder().id(1L).userId(1L).productId(10).createdAt(LocalDateTime.now()).build();
        CompareEntity c2 = CompareEntity.builder().id(2L).userId(1L).productId(20).createdAt(LocalDateTime.now()).build();

        when(compareRepo.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(c1, c2));

        ProductEntity p1 = new ProductEntity();
        p1.setId(10);
        p1.setName("Galaxy S24");
        p1.setPrice(new BigDecimal("22000000.00"));

        ProductEntity p2 = new ProductEntity();
        p2.setId(20);
        p2.setName("Xiaomi 14");
        p2.setPrice(new BigDecimal("18000000.00"));

        when(productRepo.findAllById(List.of(10, 20))).thenReturn(List.of(p1, p2));

        List<CompareDto> list = compareService.getCompareList(1L);

        assertEquals(2, list.size());
        assertEquals("Galaxy S24", list.get(0).getProductName());
        assertEquals("Xiaomi 14", list.get(1).getProductName());
    }

    @Test
    void testRemoveFromCompare() {
        compareService.removeFromCompare(1L, 10);
        verify(compareRepo, times(1)).deleteByUserIdAndProductId(1L, 10);
    }

    @Test
    void testClearCompare() {
        compareService.clearCompare(1L);
        verify(compareRepo, times(1)).deleteByUserId(1L);
    }
}
