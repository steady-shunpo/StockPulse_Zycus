package com.stockpulse.service;

import com.stockpulse.domain.AdvisoryResult;
import com.stockpulse.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Strategy selector that chooses which CommerceAdvisor implementation to use
 * based on configuration properties.
 */
@Service
public class CommerceAdvisorSelector implements CommerceAdvisor {
    
    @Autowired
    private RuleBasedCommerceAdvisor ruleBasedAdvisor;
    
    @Autowired
    private AICommerceAdvisor aiAdvisor;
    
    @Value("${commerce.advisor.strategy:rule-based}")
    private String strategy;
    
    @Override
    public AdvisoryResult generateRecommendations(Product product) {
        switch (strategy.toLowerCase()) {
            case "ai":
                return aiAdvisor.generateRecommendations(product);
            case "rule-based":
            default:
                return ruleBasedAdvisor.generateRecommendations(product);
        }
    }
    
    /**
     * Get the currently active strategy
     */
    public String getActiveStrategy() {
        return strategy;
    }
}