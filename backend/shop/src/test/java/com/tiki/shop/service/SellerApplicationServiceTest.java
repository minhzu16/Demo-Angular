package com.tiki.shop.service;

import com.tiki.common.entity.SellerApplication;
import com.tiki.common.repository.SellerApplicationRepository;
import com.tiki.shop.client.AuthClient;
import com.tiki.shop.entity.ShopEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerApplicationServiceTest {

    @Mock
    private SellerApplicationRepository repository;

    @Mock
    private AuthClient authClient;

    @Mock
    private ShopService shopService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SellerApplicationService applicationService;

    @Test
    @DisplayName("createApplication - throws if pending exists")
    void createApplication_ThrowsIfPendingExists() {
        when(repository.existsByUserIdAndStatus(10L, SellerApplication.Status.PENDING)).thenReturn(true);

        SellerApplication app = new SellerApplication();
        assertThatThrownBy(() -> applicationService.createApplication(10L, app))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already have a pending");
    }

    @Test
    @DisplayName("createApplication - saves if valid")
    void createApplication_Saves() {
        when(repository.existsByUserIdAndStatus(10L, SellerApplication.Status.PENDING)).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SellerApplication app = new SellerApplication();
        SellerApplication result = applicationService.createApplication(10L, app);

        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(SellerApplication.Status.PENDING);
    }

    @Test
    @DisplayName("approveApplication - approves, updates role, creates shop")
    void approveApplication_Success() {
        SellerApplication app = new SellerApplication();
        app.setId(1L);
        app.setUserId(10L);
        app.setStatus(SellerApplication.Status.PENDING);
        app.setShopName("Test Shop");

        when(repository.findById(1L)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        SellerApplication result = applicationService.approveApplication(1L, 99L);

        assertThat(result.getStatus()).isEqualTo(SellerApplication.Status.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo(99L);

        verify(authClient).updateUserRole(10L, "SELLER");
        verify(shopService).createOrUpdateShop(any(ShopEntity.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("rejectApplication - rejects application")
    void rejectApplication_Success() {
        SellerApplication app = new SellerApplication();
        app.setId(1L);
        app.setUserId(10L);
        app.setStatus(SellerApplication.Status.PENDING);

        when(repository.findById(1L)).thenReturn(Optional.of(app));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SellerApplication result = applicationService.rejectApplication(1L, 99L, "Not enough info");

        assertThat(result.getStatus()).isEqualTo(SellerApplication.Status.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Not enough info");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), org.mockito.ArgumentMatchers.<Object>any());
    }
}
