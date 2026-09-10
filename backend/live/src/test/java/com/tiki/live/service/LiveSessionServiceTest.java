package com.tiki.live.service;

import com.tiki.live.dto.*;
import com.tiki.live.entity.LiveProductPinEntity;
import com.tiki.live.entity.LiveSessionEntity;
import com.tiki.live.repository.LiveProductPinRepository;
import com.tiki.live.repository.LiveSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveSessionServiceTest {

    @Mock
    private LiveSessionRepository liveSessionRepository;

    @Mock
    private LiveProductPinRepository liveProductPinRepository;

    @InjectMocks
    private LiveSessionService liveSessionService;

    private LiveSessionEntity liveSession;

    @BeforeEach
    void setUp() {
        liveSession = LiveSessionEntity.builder()
                .id(1L)
                .sellerId(10L)
                .title("Flash Sale Livestream")
                .streamKey("live_testkey123")
                .status(LiveSessionEntity.LiveStatus.SCHEDULED)
                .viewerCount(0)
                .peakViewers(0)
                .likeCount(0)
                .build();
    }

    @Test
    void testCreateSession_Success() {
        LiveSessionCreateRequest req = LiveSessionCreateRequest.builder()
                .title("Mega Live Deal 11.11")
                .description("Giảm giá tới 50%")
                .build();

        when(liveSessionRepository.save(any(LiveSessionEntity.class))).thenAnswer(inv -> {
            LiveSessionEntity s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        LiveSessionResponseDto res = liveSessionService.createSession(10L, req);

        assertNotNull(res);
        assertEquals(100L, res.getId());
        assertEquals("Mega Live Deal 11.11", res.getTitle());
        assertEquals(LiveSessionEntity.LiveStatus.SCHEDULED, res.getStatus());
        assertTrue(res.getStreamKey().startsWith("live_"));
    }

    @Test
    void testStartAndEndLive_Lifecycle() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(liveSession));
        when(liveSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Start Live
        LiveSessionResponseDto started = liveSessionService.startLive(1L, 10L);
        assertEquals(LiveSessionEntity.LiveStatus.LIVE, started.getStatus());
        assertNotNull(started.getStartedAt());

        // End Live
        LiveSessionResponseDto ended = liveSessionService.endLive(1L, 10L);
        assertEquals(LiveSessionEntity.LiveStatus.ENDED, ended.getStatus());
        assertNotNull(ended.getEndedAt());
    }

    @Test
    void testStartLive_UnauthorizedSeller_Throws() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(liveSession));

        assertThrows(SecurityException.class, () ->
                liveSessionService.startLive(1L, 999L)); // Not the seller
    }

    @Test
    void testPinProduct_Success() {
        LivePinProductRequest req = LivePinProductRequest.builder()
                .productId(200L)
                .productName("Tai nghe Bluetooth")
                .originalPrice(new BigDecimal("500000.00"))
                .livePrice(new BigDecimal("299000.00"))
                .stockLimit(50)
                .build();

        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(liveSession));
        when(liveProductPinRepository.findBySessionIdAndProductId(1L, 200L)).thenReturn(Optional.empty());
        when(liveProductPinRepository.save(any(LiveProductPinEntity.class))).thenAnswer(inv -> {
            LiveProductPinEntity p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        LiveProductPinDto pin = liveSessionService.pinProduct(1L, 10L, req);

        assertNotNull(pin);
        assertEquals(10L, pin.getId());
        assertEquals(200L, pin.getProductId());
        assertEquals(new BigDecimal("299000.00"), pin.getLivePrice());
        assertTrue(pin.getIsCurrentlyPinned());
    }

    @Test
    void testQuickBuy_Success() {
        liveSession.setStatus(LiveSessionEntity.LiveStatus.LIVE);

        LiveProductPinEntity pin = LiveProductPinEntity.builder()
                .id(10L)
                .sessionId(1L)
                .productId(200L)
                .productName("Tai nghe Bluetooth")
                .livePrice(new BigDecimal("299000.00"))
                .stockLimit(10)
                .soldCount(2)
                .isCurrentlyPinned(true)
                .build();

        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(liveSession));
        when(liveProductPinRepository.findBySessionIdAndProductId(1L, 200L)).thenReturn(Optional.of(pin));
        when(liveProductPinRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LiveQuickBuyResponseDto res = liveSessionService.quickBuy(1L, 200L, 50L, 2);

        assertNotNull(res);
        assertEquals(200L, res.getProductId());
        assertEquals(2, res.getQuantity());
        assertEquals(new BigDecimal("598000.00"), res.getTotalPrice());
        assertEquals(4, pin.getSoldCount()); // 2 + 2 = 4
        assertTrue(res.getQuickBuyCheckoutToken().startsWith("LIVE_QB_"));
    }

    @Test
    void testQuickBuy_ExceedsStockLimit_Throws() {
        liveSession.setStatus(LiveSessionEntity.LiveStatus.LIVE);

        LiveProductPinEntity pin = LiveProductPinEntity.builder()
                .sessionId(1L)
                .productId(200L)
                .stockLimit(5)
                .soldCount(4) // Only 1 left
                .isCurrentlyPinned(true)
                .build();

        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(liveSession));
        when(liveProductPinRepository.findBySessionIdAndProductId(1L, 200L)).thenReturn(Optional.of(pin));

        // Try to buy 2 -> exceeds limit 5
        assertThrows(IllegalStateException.class, () ->
                liveSessionService.quickBuy(1L, 200L, 50L, 2));
    }

    @Test
    void testUpdateViewerCount_And_Peak() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(liveSession));
        when(liveSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        liveSessionService.updateViewerCount(1L, 1500);

        assertEquals(1500, liveSession.getViewerCount());
        assertEquals(1500, liveSession.getPeakViewers());

        // Count drops to 1200 -> peak remains 1500
        liveSessionService.updateViewerCount(1L, 1200);
        assertEquals(1200, liveSession.getViewerCount());
        assertEquals(1500, liveSession.getPeakViewers());
    }
}
