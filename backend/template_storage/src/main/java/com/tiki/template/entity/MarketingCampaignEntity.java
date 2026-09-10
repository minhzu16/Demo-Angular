package com.tiki.template.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_campaigns", indexes = {
    @Index(name = "idx_campaign_code", columnList = "code"),
    @Index(name = "idx_campaign_status", columnList = "status"),
    @Index(name = "idx_campaign_dates", columnList = "start_date, end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignEntity {

    public enum CampaignStatus {
        DRAFT,
        ACTIVE,
        PAUSED,
        EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "landing_page_url", length = 500)
    private String landingPageUrl;

    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isCurrentlyRunning() {
        if (status != CampaignStatus.ACTIVE) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        return true;
    }
}
