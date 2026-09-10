package com.tiki.template.service;

import com.tiki.template.dto.*;
import com.tiki.template.entity.CampaignBannerEntity;
import com.tiki.template.entity.MarketingCampaignEntity;
import com.tiki.template.repository.CampaignBannerRepository;
import com.tiki.template.repository.MarketingCampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingServiceTest {

    @Mock
    private MarketingCampaignRepository campaignRepository;

    @Mock
    private CampaignBannerRepository bannerRepository;

    @InjectMocks
    private MarketingService marketingService;

    @Test
    @DisplayName("createCampaign - creates campaign successfully")
    void testCreateCampaign_Success() {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .code("CAMP-TET-2026")
                .title("Chiến dịch Tết 2026")
                .description("Siêu sale đầu năm")
                .bannerImageUrl("https://img.tiki.vn/tet2026.jpg")
                .discountPercentage(20)
                .voucherCode("TET2026")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        when(campaignRepository.existsByCode("CAMP-TET-2026")).thenReturn(false);
        when(campaignRepository.save(any(MarketingCampaignEntity.class))).thenAnswer(i -> {
            MarketingCampaignEntity e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        MarketingCampaignDto result = marketingService.createCampaign(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("CAMP-TET-2026");
        assertThat(result.getTitle()).isEqualTo("Chiến dịch Tết 2026");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(campaignRepository).save(any(MarketingCampaignEntity.class));
    }

    @Test
    @DisplayName("createCampaign - duplicate code throws IllegalArgumentException")
    void testCreateCampaign_DuplicateCode() {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .code("CAMP-EXISTING")
                .title("Trùng mã")
                .build();

        when(campaignRepository.existsByCode("CAMP-EXISTING")).thenReturn(true);

        assertThatThrownBy(() -> marketingService.createCampaign(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("createCampaign - invalid dates (start after end) throws IllegalArgumentException")
    void testCreateCampaign_InvalidDates() {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .code("CAMP-INVALID-DATE")
                .title("Sai ngày")
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        when(campaignRepository.existsByCode("CAMP-INVALID-DATE")).thenReturn(false);

        assertThatThrownBy(() -> marketingService.createCampaign(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày bắt đầu không được sau ngày kết thúc");
    }

    @Test
    @DisplayName("createBanner - creates banner with position HOME_HERO")
    void testCreateBanner_Success() {
        CreateBannerRequest request = CreateBannerRequest.builder()
                .campaignId(1L)
                .title("Banner Tết")
                .imageUrl("https://img.tiki.vn/banner1.png")
                .targetUrl("https://tiki.vn/events/tet")
                .position("HOME_HERO")
                .displayOrder(1)
                .build();

        when(bannerRepository.save(any(CampaignBannerEntity.class))).thenAnswer(i -> {
            CampaignBannerEntity b = i.getArgument(0);
            b.setId(10L);
            return b;
        });

        CampaignBannerDto result = marketingService.createBanner(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getPosition()).isEqualTo("HOME_HERO");
        assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("getActiveBannersByPosition - returns active banners filtered by position")
    void testGetActiveBannersByPosition() {
        CampaignBannerEntity banner = CampaignBannerEntity.builder()
                .id(10L)
                .title("Hero 1")
                .imageUrl("https://img.tiki.vn/hero1.png")
                .position("HOME_HERO")
                .displayOrder(1)
                .active(true)
                .build();

        when(bannerRepository.findByPositionIgnoreCaseAndActiveTrueOrderByDisplayOrderAsc("HOME_HERO"))
                .thenReturn(List.of(banner));

        List<CampaignBannerDto> list = marketingService.getActiveBannersByPosition("HOME_HERO");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Hero 1");
    }

    @Test
    @DisplayName("toggleBannerStatus - toggles banner active state")
    void testToggleBannerStatus() {
        CampaignBannerEntity banner = CampaignBannerEntity.builder()
                .id(10L)
                .active(true)
                .build();

        when(bannerRepository.findById(10L)).thenReturn(Optional.of(banner));
        when(bannerRepository.save(any(CampaignBannerEntity.class))).thenAnswer(i -> i.getArgument(0));

        marketingService.toggleBannerStatus(10L);

        assertThat(banner.getActive()).isFalse();
    }
}
