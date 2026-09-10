package com.tiki.template.repository;

import com.tiki.template.entity.MarketingCampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaignEntity, Long> {
    Optional<MarketingCampaignEntity> findByCode(String code);
    List<MarketingCampaignEntity> findByStatus(MarketingCampaignEntity.CampaignStatus status);
    boolean existsByCode(String code);
    List<MarketingCampaignEntity> findByStatusAndStartDateBeforeAndEndDateAfter(
            MarketingCampaignEntity.CampaignStatus status, LocalDateTime start, LocalDateTime end);
}
