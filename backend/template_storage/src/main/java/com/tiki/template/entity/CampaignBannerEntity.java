package com.tiki.template.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_banners", indexes = {
    @Index(name = "idx_banner_pos_active", columnList = "position, is_active"),
    @Index(name = "idx_banner_campaign", columnList = "campaign_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignBannerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String position = "HOME_HERO"; // HOME_HERO, HOME_MIDDLE, CATEGORY_TOP, POPUP

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
