package com.tiki.template.repository;

import com.tiki.template.entity.CampaignBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignBannerRepository extends JpaRepository<CampaignBannerEntity, Long> {
    List<CampaignBannerEntity> findByPositionIgnoreCaseAndActiveTrueOrderByDisplayOrderAsc(String position);
    List<CampaignBannerEntity> findByActiveTrueOrderByDisplayOrderAsc();
    List<CampaignBannerEntity> findByCampaignId(Long campaignId);
}
