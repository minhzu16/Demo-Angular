package com.tiki.analytics.service;

import com.tiki.analytics.dto.CustomerMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Customer Analytics Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAnalyticsService {
    
    /**
     * Get customer acquisition metrics
     */
    @Cacheable(value = "customerAcquisition", key = "#shopId + '_' + #days")
    public CustomerMetricsDTO getCustomerAcquisition(Long shopId, int days) {
        log.info("Getting customer acquisition for shop {} in last {} days", shopId, days);
        
        // Mock implementation
        Random random = new Random(shopId);
        int totalCustomers = 1000 + random.nextInt(4000);
        int newCustomers = (int) (totalCustomers * 0.3);
        int returningCustomers = totalCustomers - newCustomers;
        
        return CustomerMetricsDTO.builder()
                .totalCustomers(totalCustomers)
                .newCustomers(newCustomers)
                .returningCustomers(returningCustomers)
                .retentionRate(65.0 + random.nextDouble() * 20)
                .averageLifetimeValue(new BigDecimal(500 + random.nextInt(1500)))
                .repeatPurchaseRate(35.0 + random.nextDouble() * 25)
                .averageOrdersPerCustomer(2 + random.nextInt(5))
                .period(days)
                .build();
    }
    
    /**
     * Get customer lifetime value distribution
     */
    @Cacheable(value = "clvDistribution", key = "#shopId")
    public Map<String, Object> getCustomerLifetimeValue(Long shopId) {
        log.info("Getting CLV distribution for shop {}", shopId);
        
        Random random = new Random(shopId);
        Map<String, Object> clv = new HashMap<>();
        clv.put("averageCLV", 800 + random.nextInt(1200));
        clv.put("medianCLV", 600 + random.nextInt(800));
        clv.put("topCustomersCLV", 5000 + random.nextInt(15000));
        clv.put("customerSegments", Arrays.asList(
            Map.of("segment", "High Value", "count", 150 + random.nextInt(200), "avgCLV", 3000 + random.nextInt(2000)),
            Map.of("segment", "Medium Value", "count", 500 + random.nextInt(500), "avgCLV", 1000 + random.nextInt(1000)),
            Map.of("segment", "Low Value", "count", 800 + random.nextInt(1000), "avgCLV", 200 + random.nextInt(300))
        ));
        
        return clv;
    }
    
    /**
     * Get customer retention metrics
     */
    @Cacheable(value = "customerRetention", key = "#shopId + '_' + #days")
    public Map<String, Object> getRetentionMetrics(Long shopId, int days) {
        log.info("Getting retention metrics for shop {} in last {} days", shopId, days);
        
        Random random = new Random(shopId);
        Map<String, Object> retention = new HashMap<>();
        retention.put("retentionRate", 60.0 + random.nextDouble() * 25);
        retention.put("churnRate", 10.0 + random.nextDouble() * 15);
        retention.put("repeatPurchaseRate", 40.0 + random.nextDouble() * 20);
        retention.put("daysSinceLastPurchase", 15 + random.nextInt(30));
        retention.put("cohortAnalysis", Arrays.asList(
            Map.of("cohort", "Month 1", "retention", 100.0),
            Map.of("cohort", "Month 2", "retention", 75.0 + random.nextDouble() * 10),
            Map.of("cohort", "Month 3", "retention", 60.0 + random.nextDouble() * 10),
            Map.of("cohort", "Month 4", "retention", 50.0 + random.nextDouble() * 10),
            Map.of("cohort", "Month 5", "retention", 45.0 + random.nextDouble() * 10),
            Map.of("cohort", "Month 6", "retention", 40.0 + random.nextDouble() * 10)
        ));
        
        return retention;
    }
    
    /**
     * Get customer purchase patterns
     */
    @Cacheable(value = "purchasePatterns", key = "#shopId + '_' + #days")
    public Map<String, Object> getPurchasePatterns(Long shopId, int days) {
        log.info("Getting purchase patterns for shop {} in last {} days", shopId, days);
        
        Random random = new Random(shopId);
        Map<String, Object> patterns = new HashMap<>();
        patterns.put("averageOrderValue", 250 + random.nextInt(300));
        patterns.put("purchaseFrequency", 1.5 + random.nextDouble() * 2);
        patterns.put("preferredPaymentMethod", "COD");
        patterns.put("peakPurchaseDay", "Saturday");
        patterns.put("peakPurchaseHour", "20:00");
        patterns.put("categoryPreferences", Arrays.asList("Electronics", "Fashion", "Home"));
        
        return patterns;
    }
}
