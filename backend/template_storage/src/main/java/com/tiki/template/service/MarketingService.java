package com.tiki.template.service;

import com.tiki.template.dto.*;
import com.tiki.template.entity.CampaignBannerEntity;
import com.tiki.template.entity.MarketingCampaignEntity;
import com.tiki.template.repository.CampaignBannerRepository;
import com.tiki.template.repository.MarketingCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketingService {

    private final MarketingCampaignRepository campaignRepository;
    private final CampaignBannerRepository bannerRepository;

    @Transactional
    public MarketingCampaignDto createCampaign(CreateCampaignRequest request) {
        log.info("Creating marketing campaign: code={}, title={}", request.getCode(), request.getTitle());

        if (campaignRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã chiến dịch " + request.getCode() + " đã tồn tại.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null 
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        MarketingCampaignEntity entity = MarketingCampaignEntity.builder()
                .code(request.getCode().trim().toUpperCase())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .bannerImageUrl(request.getBannerImageUrl())
                .landingPageUrl(request.getLandingPageUrl())
                .discountPercentage(request.getDiscountPercentage())
                .voucherCode(request.getVoucherCode())
                .status(MarketingCampaignEntity.CampaignStatus.ACTIVE)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return toDto(campaignRepository.save(entity));
    }

    public List<MarketingCampaignDto> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MarketingCampaignDto> getActiveCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findByStatusAndStartDateBeforeAndEndDateAfter(
                MarketingCampaignEntity.CampaignStatus.ACTIVE, now, now).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public MarketingCampaignDto getCampaignByCode(String code) {
        return campaignRepository.findByCode(code.trim().toUpperCase())
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch với mã: " + code));
    }

    @Transactional
    public MarketingCampaignDto updateCampaignStatus(Long id, MarketingCampaignEntity.CampaignStatus status) {
        MarketingCampaignEntity campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch: " + id));
        campaign.setStatus(status);
        return toDto(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignBannerDto createBanner(CreateBannerRequest request) {
        log.info("Creating campaign banner: title={}, position={}", request.getTitle(), request.getPosition());

        CampaignBannerEntity entity = CampaignBannerEntity.builder()
                .campaignId(request.getCampaignId())
                .title(request.getTitle().trim())
                .imageUrl(request.getImageUrl().trim())
                .targetUrl(request.getTargetUrl())
                .position(request.getPosition() != null ? request.getPosition().trim().toUpperCase() : "HOME_HERO")
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(true)
                .build();

        return toDto(bannerRepository.save(entity));
    }

    public List<CampaignBannerDto> getActiveBannersByPosition(String position) {
        if (position == null || position.isBlank()) {
            return bannerRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return bannerRepository.findByPositionIgnoreCaseAndActiveTrueOrderByDisplayOrderAsc(position).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CampaignBannerDto> getAllBanners() {
        return bannerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleBannerStatus(Long bannerId) {
        CampaignBannerEntity banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy banner: " + bannerId));
        banner.setActive(!Boolean.TRUE.equals(banner.getActive()));
        bannerRepository.save(banner);
        log.info("Toggled banner {} active status to {}", bannerId, banner.getActive());
    }

    private MarketingCampaignDto toDto(MarketingCampaignEntity entity) {
        return MarketingCampaignDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .bannerImageUrl(entity.getBannerImageUrl())
                .landingPageUrl(entity.getLandingPageUrl())
                .discountPercentage(entity.getDiscountPercentage())
                .voucherCode(entity.getVoucherCode())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .currentlyRunning(entity.isCurrentlyRunning())
                .build();
    }

    private CampaignBannerDto toDto(CampaignBannerEntity entity) {
        return CampaignBannerDto.builder()
                .id(entity.getId())
                .campaignId(entity.getCampaignId())
                .title(entity.getTitle())
                .imageUrl(entity.getImageUrl())
                .targetUrl(entity.getTargetUrl())
                .position(entity.getPosition())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.getActive())
                .build();
    }
}
