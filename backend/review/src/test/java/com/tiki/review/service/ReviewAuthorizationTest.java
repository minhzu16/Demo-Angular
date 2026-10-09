package com.tiki.review.service;

import com.tiki.review.client.OrderClient;
import com.tiki.review.client.ProductClient;
import com.tiki.review.client.ShopClient;
import com.tiki.review.entity.ReviewEntity;
import com.tiki.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAuthorizationTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderClient orderClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ReviewMapper reviewMapper;
    @Mock private ProductClient productClient;
    @Mock private ShopClient shopClient;

    private ReviewEntity review(long id, long userId, long productId) {
        ReviewEntity e = new ReviewEntity();
        e.setId(id);
        e.setUserId(userId);
        e.setProductId(productId);
        return e;
    }

    @Test
    @DisplayName("Chỉ tác giả (hoặc admin) xóa được đánh giá")
    void deleteReview_onlyAuthorOrAdmin() {
        ReviewCommandService service = new ReviewCommandService(reviewRepository, orderClient, rabbitTemplate, reviewMapper);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review(5, 10, 100)));

        assertThatThrownBy(() -> service.deleteReview(5L, 99L, false)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.deleteReview(5L, null, false)).isInstanceOf(SecurityException.class);
        verify(reviewRepository, never()).deleteById(5L);

        assertThat(service.deleteReview(5L, 10L, false)).isTrue();
        assertThat(service.deleteReview(5L, 1L, true)).isTrue();
        verify(reviewRepository, org.mockito.Mockito.times(2)).deleteById(5L);
    }

    @Test
    @DisplayName("Đánh giá không tồn tại -> false (không lộ thông tin quyền)")
    void deleteReview_missing() {
        ReviewCommandService service = new ReviewCommandService(reviewRepository, orderClient, rabbitTemplate, reviewMapper);
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());
        assertThat(service.deleteReview(404L, 10L, false)).isFalse();
    }

    @Test
    @DisplayName("Phản hồi của shop: admin được; seller chỉ khi sở hữu shop bán sản phẩm; người mua/khách bị từ chối")
    void canReply_requiresOwnerSellerOrAdmin() {
        ReviewAccessService access = new ReviewAccessService(reviewRepository, productClient, shopClient);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review(5, 10, 100)));
        when(productClient.getProduct(100L)).thenReturn(new ProductClient.ProductRef(7L));
        when(shopClient.getShopBySeller(50L)).thenReturn(new ShopClient.ShopRef(7L));   // owns shop 7
        when(shopClient.getShopBySeller(51L)).thenReturn(new ShopClient.ShopRef(8L));   // owns another shop

        assertThat(access.canReply(5L, 1L, "ADMIN")).isTrue();
        assertThat(access.canReply(5L, 50L, "SELLER")).isTrue();
        assertThat(access.canReply(5L, 51L, "SELLER")).isFalse();      // seller of a different shop
        assertThat(access.canReply(5L, 10L, "BUYER")).isFalse();       // plain buyer
        assertThat(access.canReply(5L, null, "SELLER")).isFalse();
    }

    @Test
    @DisplayName("Không xác minh được (product/shop-service lỗi) -> từ chối")
    void canReply_failsClosed() {
        ReviewAccessService access = new ReviewAccessService(reviewRepository, productClient, shopClient);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review(5, 10, 100)));
        when(productClient.getProduct(100L)).thenThrow(new RuntimeException("down"));

        assertThat(access.canReply(5L, 50L, "SELLER")).isFalse();
    }
}
