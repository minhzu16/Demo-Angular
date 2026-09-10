package com.tiki.template.controller;

import com.tiki.template.dto.CreateCampaignRequest;
import com.tiki.template.dto.MarketingCampaignDto;
import com.tiki.template.entity.MarketingCampaignEntity;
import com.tiki.template.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketing/campaigns")
@RequiredArgsConstructor
@Slf4j
public class MarketingCampaignController {

    private final MarketingService marketingService;

    @PostMapping
    public ResponseEntity<MarketingCampaignDto> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request) {
        log.info("Received request to create campaign: {}", request.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(marketingService.createCampaign(request));
    }

    @GetMapping
    public ResponseEntity<List<MarketingCampaignDto>> getAllCampaigns() {
        return ResponseEntity.ok(marketingService.getAllCampaigns());
    }

    @GetMapping("/active")
    public ResponseEntity<List<MarketingCampaignDto>> getActiveCampaigns() {
        return ResponseEntity.ok(marketingService.getActiveCampaigns());
    }

    @GetMapping("/{code}")
    public ResponseEntity<MarketingCampaignDto> getCampaignByCode(@PathVariable String code) {
        return ResponseEntity.ok(marketingService.getCampaignByCode(code));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MarketingCampaignDto> updateStatus(
            @PathVariable Long id,
            @RequestParam MarketingCampaignEntity.CampaignStatus status) {
        log.info("Updating campaign {} status to {}", id, status);
        return ResponseEntity.ok(marketingService.updateCampaignStatus(id, status));
    }
}
