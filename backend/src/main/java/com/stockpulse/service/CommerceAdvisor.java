package com.stockpulse.service;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.AdvisoryResult;

/**
 * Unified contract for commerce advisory services that provides both
 * pricing and reorder recommendations for products.
 */
public interface CommerceAdvisor {
    
    /**
     * Generate pricing and reorder recommendations for a product
     * 
     * @param product The product to generate recommendations for
     * @return AdvisoryResult containing both pricing and reorder suggestions
     */
    AdvisoryResult generateRecommendations(Product product);
}