package com.stockpulse.service;

import com.stockpulse.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rule-based implementation of CommerceAdvisor that provides pricing and reorder recommendations
 * based on predefined business rules.
 */
@Service
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Override
    public AdvisoryResult generateRecommendations(Product product) {
        PricingSuggestion pricingSuggestion = generatePricingRecommendation(product);
        ReorderSuggestion reorderSuggestion = generateReorderRecommendation(product);
        
        return new AdvisoryResult(pricingSuggestion, reorderSuggestion);
    }
    
    /**
     * Rule-based pricing strategy:
     * - If stock < reorder threshold, recommend 10% price increase
     * - If demand velocity > 2× category average, recommend 5% increase
     * - Otherwise HOLD
     */
    private PricingSuggestion generatePricingRecommendation(Product product) {
        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setProduct(product);
        suggestion.setCurrentPrice(product.getCurrentPrice());
        suggestion.setCreatedAt(LocalDateTime.now());
        suggestion.setStatus(SuggestionStatus.PENDING);
        
        // Calculate category average demand velocity
        List<Product> categoryProducts = productRepository.findByCategory(product.getCategory());
        double categoryAvgDemandVelocity = categoryProducts.stream()
                .mapToInt(Product::getDemandVelocity)
                .average()
                .orElse(0.0);
        
        // Apply pricing rules
        if (product.getStockLevel() < product.getReorderThreshold()) {
            // Low stock - increase price by 10%
            BigDecimal newPrice = product.getCurrentPrice().multiply(new BigDecimal("1.10"));
            suggestion.setRecommendedPrice(newPrice);
            suggestion.setChangeDirection(ChangeDirection.INCREASE);
            suggestion.setTriggerReason(TriggerReason.INVENTORY_LOW);
            suggestion.setReasoning("Stock level (" + product.getStockLevel() + 
                    ") is below reorder threshold (" + product.getReorderThreshold() + 
                    "). Recommending 10% price increase.");
            suggestion.setConfidence(0.9);
        } else if (product.getDemandVelocity() > categoryAvgDemandVelocity * 2) {
            // High demand - increase price by 5%
            BigDecimal newPrice = product.getCurrentPrice().multiply(new BigDecimal("1.05"));
            suggestion.setRecommendedPrice(newPrice);
            suggestion.setChangeDirection(ChangeDirection.INCREASE);
            suggestion.setTriggerReason(TriggerReason.DEMAND_SPIKE);
            suggestion.setReasoning("Demand velocity (" + product.getDemandVelocity() + 
                    ") is more than 2x category average (" + categoryAvgDemandVelocity + 
                    "). Recommending 5% price increase.");
            suggestion.setConfidence(0.8);
        } else {
            // Hold current price
            suggestion.setRecommendedPrice(product.getCurrentPrice());
            suggestion.setChangeDirection(ChangeDirection.HOLD);
            suggestion.setTriggerReason(TriggerReason.INITIAL);
            suggestion.setReasoning("No significant factors affecting pricing. Maintaining current price.");
            suggestion.setConfidence(0.7);
        }
        
        return suggestion;
    }
    
    /**
     * Rule-based reorder strategy:
     * - Recommend quantity = (reorder threshold × 3) − current stock, minimum 1
     */
    private ReorderSuggestion generateReorderRecommendation(Product product) {
        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setProduct(product);
        suggestion.setCurrentStock(product.getStockLevel());
        suggestion.setCreatedAt(LocalDateTime.now());
        suggestion.setStatus(SuggestionStatus.PENDING);
        
        // Calculate recommended reorder quantity
        int recommendedQuantity = Math.max(1, (product.getReorderThreshold() * 3) - product.getStockLevel());
        
        suggestion.setRecommendedQuantity(recommendedQuantity);
        suggestion.setSuggestedLeadTimeDays(7); // Default lead time
        
        if (product.getStockLevel() < product.getReorderThreshold()) {
            suggestion.setTriggerReason(TriggerReason.INVENTORY_LOW);
            suggestion.setReasoning("Current stock level (" + product.getStockLevel() + 
                    ") is below reorder threshold (" + product.getReorderThreshold() + 
                    "). Recommending reorder of " + recommendedQuantity + " units.");
            suggestion.setConfidence(0.95);
        } else {
            suggestion.setTriggerReason(TriggerReason.INITIAL);
            suggestion.setReasoning("Proactive reorder recommendation to maintain optimal inventory levels. " +
                    "Recommended quantity: " + recommendedQuantity + " units.");
            suggestion.setConfidence(0.6);
        }
        
        return suggestion;
    }
}