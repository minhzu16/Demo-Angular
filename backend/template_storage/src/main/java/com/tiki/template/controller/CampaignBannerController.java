package com.tiki.template.controller;

import com.tiki.template.dto.CampaignBannerDto;
import com.tiki.template.dto.CreateBannerRequest;
import com.tiki.template.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketing/banners")
@RequiredArgsConstructor
@Slf4j
public class CampaignBannerController {

    private final MarketingService marketingService;

    @PostMapping
    public ResponseEntity<CampaignBannerDto> createBanner(
            @Valid @RequestBody CreateBannerRequest request) {
        log.info("Received request to create banner: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(marketingService.createBanner(request));
    }

    @GetMapping
    public ResponseEntity<List<CampaignBannerDto>> getAllBanners() {
        return ResponseEntity.ok(marketingService.getAllBanners());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CampaignBannerDto>> getActiveBanners(
            @RequestParam(required = false) String position) {
        return ResponseEntity.ok(marketingService.getActiveBannersByPosition(position));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleBanner(@PathVariable Long id) {
        log.info("Toggling banner {} active status", id);
        marketingService.toggleBannerStatus(id);
        return ResponseEntity.ok().build();
    }
}
