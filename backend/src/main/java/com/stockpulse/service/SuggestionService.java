package com.stockpulse.service;

import com.stockpulse.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class SuggestionService {
    
    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;
    
    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Update the status of a pricing suggestion and handle side effects
     */
    @Transactional
    public PricingSuggestion updatePricingSuggestionStatus(Long suggestionId, SuggestionStatus status) {
        Optional<PricingSuggestion> suggestionOpt = pricingSuggestionRepository.findById(suggestionId);
        if (!suggestionOpt.isPresent()) {
            throw new RuntimeException("Pricing suggestion not found with ID: " + suggestionId);
        }
        
        PricingSuggestion suggestion = suggestionOpt.get();
        suggestion.setStatus(status);
        
        // If accepted, update the product price
        if (status == SuggestionStatus.ACCEPTED) {
            Product product = suggestion.getProduct();
            product.updatePrice(suggestion.getRecommendedPrice());
            product.completePricingReview();
            productRepository.save(product);
        } else if (status == SuggestionStatus.REJECTED) {
            // If rejected, just complete the pricing review
            Product product = suggestion.getProduct();
            product.completePricingReview();
            productRepository.save(product);
        }
        
        return pricingSuggestionRepository.save(suggestion);
    }
    
    /**
     * Update the status of a reorder suggestion and handle side effects
     */
    @Transactional
    public ReorderSuggestion updateReorderSuggestionStatus(Long suggestionId, SuggestionStatus status) {
        Optional<ReorderSuggestion> suggestionOpt = reorderSuggestionRepository.findById(suggestionId);
        if (!suggestionOpt.isPresent()) {
            throw new RuntimeException("Reorder suggestion not found with ID: " + suggestionId);
        }
        
        ReorderSuggestion suggestion = suggestionOpt.get();
        suggestion.setStatus(status);
        
        // If accepted, update the product stock
        if (status == SuggestionStatus.ACCEPTED) {
            Product product = suggestion.getProduct();
            product.replenishStock(suggestion.getRecommendedQuantity());
            productRepository.save(product);
        }
        // If rejected, no action needed on the product
        
        return reorderSuggestionRepository.save(suggestion);
    }
    
    /**
     * Get pending suggestions for both pricing and reorder
     */
    public List<PricingSuggestion> getPendingPricingSuggestions() {
        return pricingSuggestionRepository.findByStatus(SuggestionStatus.PENDING);
    }
    
    public List<ReorderSuggestion> getPendingReorderSuggestions() {
        return reorderSuggestionRepository.findByStatus(SuggestionStatus.PENDING);
    }
}