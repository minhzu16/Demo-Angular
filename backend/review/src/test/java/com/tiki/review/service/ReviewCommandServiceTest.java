package com.tiki.review.service;

import com.tiki.review.client.OrderClient;
import com.tiki.review.dto.CreateReviewRequest;
import com.tiki.review.dto.ReviewDto;
import com.tiki.review.entity.ReviewEntity;
import com.tiki.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCommandService – unit tests")
class ReviewCommandServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewCommandService commandService;

    private CreateReviewRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreateReviewRequest.builder()
                .productId(101L)
                .userId(1L)
                .userName("Alice")
                .rating(5)
                .comment("Excellent!")
                .build();
    }

    // helper: disambiguate the 3-arg convertAndSend(String, String, Object)
    private void verifyEventPublished() {
        // Use explicit cast to select convertAndSend(String, String, Object)
        verify(rabbitTemplate).convertAndSend(
                anyString(), anyString(), ArgumentMatchers.<Object>any());
    }

    private void verifyEventNeverPublished() {
        verify(rabbitTemplate, never()).convertAndSend(
                anyString(), anyString(), ArgumentMatchers.<Object>any());
    }

    // ── createReview ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createReview – succeeds when user has purchased")
    void createReview_Success_WhenPurchased() {
        ReviewEntity saved = ReviewEntity.builder()
                .id(1L).productId(101L).userId(1L).rating(5).comment("Excellent!").build();

        when(orderClient.checkUserPurchasedProduct(101L, 1L)).thenReturn(true);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(saved);

        ReviewDto result = commandService.createReview(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(101L);
        assertThat(result.getRating()).isEqualTo(5);
        verify(reviewRepository).save(any(ReviewEntity.class));
        verifyEventPublished();
    }

    @Test
    @DisplayName("createReview – throws when user has NOT purchased")
    void createReview_Throws_WhenNotPurchased() {
        when(orderClient.checkUserPurchasedProduct(101L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> commandService.createReview(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("phải mua sản phẩm");

        verify(reviewRepository, never()).save(any());
        verifyEventNeverPublished();
    }

    @Test
    @DisplayName("createReview – throws when orderClient returns null")
    void createReview_Throws_WhenOrderClientReturnsNull() {
        when(orderClient.checkUserPurchasedProduct(101L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> commandService.createReview(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("phải mua sản phẩm");

        verify(reviewRepository, never()).save(any());
        verifyEventNeverPublished();
    }

    @Test
    @DisplayName("createReview – throws when orderClient is unreachable")
    void createReview_Throws_WhenOrderServiceDown() {
        when(orderClient.checkUserPurchasedProduct(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> commandService.createReview(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể xác minh đơn hàng");

        verify(reviewRepository, never()).save(any());
        verifyEventNeverPublished();
    }

    @Test
    @DisplayName("createReview – saves review even when RabbitMQ publishing fails")
    void createReview_SavesReview_EvenIfRabbitFails() {
        ReviewEntity saved = ReviewEntity.builder()
                .id(2L).productId(101L).userId(1L).rating(5).build();

        when(orderClient.checkUserPurchasedProduct(101L, 1L)).thenReturn(true);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(saved);

        // Explicitly select convertAndSend(String, String, Object) overload
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), ArgumentMatchers.<Object>any());

        // Should NOT throw – failure is logged and swallowed
        ReviewDto result = commandService.createReview(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
    }

    // ── deleteReview ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteReview – delegates to repository")
    void deleteReview_DelegatesToRepository() {
        doNothing().when(reviewRepository).deleteById(5L);

        commandService.deleteReview(5L);

        verify(reviewRepository).deleteById(5L);
    }
}
