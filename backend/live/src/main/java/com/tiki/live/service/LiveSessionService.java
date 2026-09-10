package com.tiki.live.service;

import com.tiki.live.dto.*;
import com.tiki.live.entity.LiveProductPinEntity;
import com.tiki.live.entity.LiveSessionEntity;
import com.tiki.live.repository.LiveProductPinRepository;
import com.tiki.live.repository.LiveSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiveSessionService {

    private final LiveSessionRepository liveSessionRepository;
    private final LiveProductPinRepository liveProductPinRepository;

    @Transactional
    public LiveSessionResponseDto createSession(Long sellerId, LiveSessionCreateRequest req) {
        String streamKey = "live_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String streamUrl = "rtmp://live.tiki.vn/live/" + streamKey;

        LiveSessionEntity session = LiveSessionEntity.builder()
                .sellerId(sellerId)
                .shopId(req.getShopId())
                .title(req.getTitle())
                .description(req.getDescription())
                .thumbnailUrl(req.getThumbnailUrl())
                .streamKey(streamKey)
                .streamUrl(streamUrl)
                .status(LiveSessionEntity.LiveStatus.SCHEDULED)
                .scheduledAt(req.getScheduledAt() != null ? req.getScheduledAt() : LocalDateTime.now())
                .build();

        LiveSessionEntity saved = liveSessionRepository.save(session);
        log.info("Created live session id={}, title='{}' for sellerId={}", saved.getId(), saved.getTitle(), sellerId);
        return toDto(saved, null);
    }

    @Transactional
    public LiveSessionResponseDto startLive(Long sessionId, Long sellerId) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        validateSellerOwnership(session, sellerId);

        session.setStatus(LiveSessionEntity.LiveStatus.LIVE);
        session.setStartedAt(LocalDateTime.now());
        LiveSessionEntity saved = liveSessionRepository.save(session);

        log.info("Session id={} started broadcasting LIVE", sessionId);
        return toDto(saved, getCurrentlyPinnedDto(sessionId));
    }

    @Transactional
    public LiveSessionResponseDto endLive(Long sessionId, Long sellerId) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        validateSellerOwnership(session, sellerId);

        // Unpin any pinned products
        liveProductPinRepository.findBySessionIdAndIsCurrentlyPinnedTrue(sessionId)
                .ifPresent(pin -> {
                    pin.setIsCurrentlyPinned(false);
                    pin.setUnpinnedAt(LocalDateTime.now());
                    liveProductPinRepository.save(pin);
                });

        session.setStatus(LiveSessionEntity.LiveStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        LiveSessionEntity saved = liveSessionRepository.save(session);

        log.info("Session id={} broadcast ENDED", sessionId);
        return toDto(saved, null);
    }

    @Transactional
    public LiveProductPinDto pinProduct(Long sessionId, Long sellerId, LivePinProductRequest req) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        validateSellerOwnership(session, sellerId);

        // Unpin previous pinned product
        liveProductPinRepository.findBySessionIdAndIsCurrentlyPinnedTrue(sessionId)
                .ifPresent(p -> {
                    p.setIsCurrentlyPinned(false);
                    p.setUnpinnedAt(LocalDateTime.now());
                    liveProductPinRepository.save(p);
                });

        Optional<LiveProductPinEntity> optPin = liveProductPinRepository.findBySessionIdAndProductId(sessionId, req.getProductId());
        LiveProductPinEntity pin;
        if (optPin.isPresent()) {
            pin = optPin.get();
            pin.setLivePrice(req.getLivePrice());
            pin.setStockLimit(req.getStockLimit());
            pin.setIsCurrentlyPinned(true);
            pin.setPinnedAt(LocalDateTime.now());
        } else {
            pin = LiveProductPinEntity.builder()
                    .sessionId(sessionId)
                    .productId(req.getProductId())
                    .productName(req.getProductName())
                    .originalPrice(req.getOriginalPrice())
                    .livePrice(req.getLivePrice())
                    .stockLimit(req.getStockLimit())
                    .isCurrentlyPinned(true)
                    .pinnedAt(LocalDateTime.now())
                    .build();
        }

        LiveProductPinEntity saved = liveProductPinRepository.save(pin);
        log.info("Pinned product {} with livePrice={} in session {}", req.getProductId(), req.getLivePrice(), sessionId);
        return toPinDto(saved);
    }

    @Transactional
    public void unpinProduct(Long sessionId, Long productId, Long sellerId) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        validateSellerOwnership(session, sellerId);

        liveProductPinRepository.findBySessionIdAndProductId(sessionId, productId)
                .ifPresent(pin -> {
                    pin.setIsCurrentlyPinned(false);
                    pin.setUnpinnedAt(LocalDateTime.now());
                    liveProductPinRepository.save(pin);
                    log.info("Unpinned product {} in session {}", productId, sessionId);
                });
    }

    @Transactional
    public LiveQuickBuyResponseDto quickBuy(Long sessionId, Long productId, Long userId, Integer quantity) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        if (session.getStatus() != LiveSessionEntity.LiveStatus.LIVE) {
            throw new IllegalStateException("Phiên livestream hiện không phát sóng trực tiếp.");
        }

        LiveProductPinEntity pin = liveProductPinRepository.findBySessionIdAndProductId(sessionId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không có trong danh sách ghim của phiên live này"));

        if (!Boolean.TRUE.equals(pin.getIsCurrentlyPinned())) {
            throw new IllegalStateException("Sản phẩm hiện không được ghim trực tiếp.");
        }

        int buyQty = quantity != null && quantity > 0 ? quantity : 1;
        if (pin.getStockLimit() != null && (pin.getSoldCount() + buyQty > pin.getStockLimit())) {
            throw new IllegalStateException("Số lượng ưu đãi trong phiên live đã hết (Đã bán " +
                    pin.getSoldCount() + "/" + pin.getStockLimit() + ")");
        }

        pin.setSoldCount(pin.getSoldCount() + buyQty);
        liveProductPinRepository.save(pin);

        BigDecimal totalPrice = pin.getLivePrice().multiply(BigDecimal.valueOf(buyQty));
        String token = "LIVE_QB_" + UUID.randomUUID().toString().replace("-", "");

        log.info("User {} executed quick buy for product {} in session {}, qty={}, total={}",
                userId, productId, sessionId, buyQty, totalPrice);

        return LiveQuickBuyResponseDto.builder()
                .sessionId(sessionId)
                .productId(productId)
                .productName(pin.getProductName())
                .quantity(buyQty)
                .unitPrice(pin.getLivePrice())
                .totalPrice(totalPrice)
                .quickBuyCheckoutToken(token)
                .message("Đặt mua nhanh thành công với giá ưu đãi Livestream!")
                .build();
    }

    @Transactional
    public void updateViewerCount(Long sessionId, int count) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        session.setViewerCount(count);
        if (count > session.getPeakViewers()) {
            session.setPeakViewers(count);
        }
        liveSessionRepository.save(session);
    }

    @Transactional
    public void likeSession(Long sessionId) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        session.setLikeCount(session.getLikeCount() + 1);
        liveSessionRepository.save(session);
    }

    public List<LiveSessionResponseDto> getActiveSessions() {
        return liveSessionRepository.findByStatusOrderByStartedAtDesc(LiveSessionEntity.LiveStatus.LIVE).stream()
                .map(s -> toDto(s, getCurrentlyPinnedDto(s.getId())))
                .collect(Collectors.toList());
    }

    public LiveSessionResponseDto getSessionById(Long sessionId) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        return toDto(session, getCurrentlyPinnedDto(sessionId));
    }

    public LiveAnalyticsDto getSessionAnalytics(Long sessionId) {
        LiveSessionEntity session = getSessionEntity(sessionId);
        List<LiveProductPinEntity> pins = liveProductPinRepository.findBySessionIdOrderByDisplayOrderAsc(sessionId);

        int totalSold = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (LiveProductPinEntity p : pins) {
            int sold = p.getSoldCount() != null ? p.getSoldCount() : 0;
            totalSold += sold;
            revenue = revenue.add(p.getLivePrice().multiply(BigDecimal.valueOf(sold)));
        }

        return LiveAnalyticsDto.builder()
                .sessionId(sessionId)
                .totalViewers(session.getViewerCount())
                .peakViewers(session.getPeakViewers())
                .totalLikes(session.getLikeCount())
                .totalShares(session.getShareCount())
                .totalProductsSold(totalSold)
                .estimatedRevenue(revenue)
                .build();
    }

    public List<LiveProductPinDto> getSessionProducts(Long sessionId) {
        return liveProductPinRepository.findBySessionIdOrderByDisplayOrderAsc(sessionId).stream()
                .map(this::toPinDto)
                .collect(Collectors.toList());
    }

    private LiveSessionEntity getSessionEntity(Long sessionId) {
        return liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên livestream id=" + sessionId));
    }

    private void validateSellerOwnership(LiveSessionEntity session, Long sellerId) {
        if (!session.getSellerId().equals(sellerId)) {
            throw new SecurityException("Bạn không phải chủ phòng livestream này.");
        }
    }

    private LiveProductPinDto getCurrentlyPinnedDto(Long sessionId) {
        return liveProductPinRepository.findBySessionIdAndIsCurrentlyPinnedTrue(sessionId)
                .map(this::toPinDto)
                .orElse(null);
    }

    private LiveSessionResponseDto toDto(LiveSessionEntity s, LiveProductPinDto pinned) {
        return LiveSessionResponseDto.builder()
                .id(s.getId())
                .shopId(s.getShopId())
                .sellerId(s.getSellerId())
                .title(s.getTitle())
                .description(s.getDescription())
                .thumbnailUrl(s.getThumbnailUrl())
                .streamUrl(s.getStreamUrl())
                .streamKey(s.getStreamKey())
                .status(s.getStatus())
                .scheduledAt(s.getScheduledAt())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .viewerCount(s.getViewerCount())
                .peakViewers(s.getPeakViewers())
                .likeCount(s.getLikeCount())
                .shareCount(s.getShareCount())
                .createdAt(s.getCreatedAt())
                .currentlyPinnedProduct(pinned)
                .build();
    }

    private LiveProductPinDto toPinDto(LiveProductPinEntity p) {
        return LiveProductPinDto.builder()
                .id(p.getId())
                .sessionId(p.getSessionId())
                .productId(p.getProductId())
                .productName(p.getProductName())
                .originalPrice(p.getOriginalPrice())
                .livePrice(p.getLivePrice())
                .stockLimit(p.getStockLimit())
                .soldCount(p.getSoldCount())
                .isCurrentlyPinned(p.getIsCurrentlyPinned())
                .displayOrder(p.getDisplayOrder())
                .pinnedAt(p.getPinnedAt())
                .build();
    }
}
