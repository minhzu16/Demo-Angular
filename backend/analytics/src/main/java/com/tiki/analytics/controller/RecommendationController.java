package com.tiki.analytics.controller;

import com.tiki.analytics.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@PathVariable Long userId) {
        log.info("GET /recommendations/{}", userId);
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Map<String, Object>>> getTrending() {
        log.info("GET /recommendations/trending");
        return ResponseEntity.ok(recommendationService.getTrendingProducts());
    }
}
