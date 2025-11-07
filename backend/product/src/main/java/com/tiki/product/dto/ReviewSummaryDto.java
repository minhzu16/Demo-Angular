package com.tiki.product.dto;

import java.util.List;
import java.util.Map;

public class ReviewSummaryDto {
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution;
    private List<ReviewDto> recentReviews;
    
    public ReviewSummaryDto() {}
    
    public ReviewSummaryDto(Double averageRating, Long totalReviews, Map<Integer, Long> ratingDistribution, List<ReviewDto> recentReviews) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingDistribution = ratingDistribution;
        this.recentReviews = recentReviews;
    }
    
    public Double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
    
    public Long getTotalReviews() {
        return totalReviews;
    }
    
    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }
    
    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }
    
    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }
    
    public List<ReviewDto> getRecentReviews() {
        return recentReviews;
    }
    
    public void setRecentReviews(List<ReviewDto> recentReviews) {
        this.recentReviews = recentReviews;
    }
}
