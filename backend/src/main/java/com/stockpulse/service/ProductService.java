package com.stockpulse.service;

import com.stockpulse.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;
    
    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;
    
    /**
     * Record a sale for a product and evaluate triggers
     */
    @Transactional
    public Product recordSale(String productId, int quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        product.recordSale(quantity);
        
        // Evaluate triggers after sale
        evaluateTriggers(product);
        
        return productRepository.save(product);
    }
    
    /**
     * Update stock level and evaluate triggers
     */
    @Transactional
    public Product updateStockLevel(String productId, int stockLevel) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        product.setStockLevel(stockLevel);
        
        // Evaluate triggers after stock update
        evaluateTriggers(product);
        
        return productRepository.save(product);
    }
    
    /**
     * Replenish stock for a product
     */
    @Transactional
    public Product replenishStock(String productId, int quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        product.replenishStock(quantity);
        
        return productRepository.save(product);
    }
    
    /**
     * Evaluate triggers for low inventory or demand spikes
     */
    public void evaluateTriggers(Product product) {
        // Check for low inventory trigger
        if (product.getStockLevel() < product.getReorderThreshold()) {
            // Move to PRICE_REVIEW_PENDING if not already
            if (product.getStatus() != ProductStatus.PRICE_REVIEW_PENDING) {
                product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
                // Generate pricing suggestion
                generatePricingSuggestion(product, TriggerReason.INVENTORY_LOW);
            }
        }
        
        // TODO: Check for demand spike trigger
        // For now, we'll skip this as we don't have demand thresholds defined
    }
    
    /**
     * Generate a manual pricing suggestion
     */
    @Transactional
    public PricingSuggestion generateManualPricingSuggestion(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        return generatePricingSuggestion(product, TriggerReason.MANUAL);
    }
    
    /**
     * Generate a pricing suggestion for a product
     */
    private PricingSuggestion generatePricingSuggestion(Product product, TriggerReason triggerReason) {
        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setProduct(product);
        suggestion.setCurrentPrice(product.getCurrentPrice());
        // For demo purposes, we'll just suggest a 10% increase
        BigDecimal recommendedPrice = product.getCurrentPrice().multiply(new BigDecimal("1.1"));
        suggestion.setRecommendedPrice(recommendedPrice);
        suggestion.setChangeDirection(ChangeDirection.INCREASE);
        suggestion.setConfidence(0.8);
        suggestion.setReasoning("Generated based on " + triggerReason + " trigger");
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(triggerReason);
        suggestion.setCreatedAt(LocalDateTime.now());
        
        return pricingSuggestionRepository.save(suggestion);
    }
    
    /**
     * Generate a manual reorder suggestion
     */
    @Transactional
    public ReorderSuggestion generateManualReorderSuggestion(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        return generateReorderSuggestion(product, TriggerReason.MANUAL);
    }
    
    /**
     * Generate a reorder suggestion for a product
     */
    private ReorderSuggestion generateReorderSuggestion(Product product, TriggerReason triggerReason) {
        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setProduct(product);
        suggestion.setCurrentStock(product.getStockLevel());
        // For demo purposes, we'll just suggest ordering 2x the reorder threshold
        int recommendedQuantity = product.getReorderThreshold() * 2;
        suggestion.setRecommendedQuantity(recommendedQuantity);
        suggestion.setSuggestedLeadTimeDays(5); // Default lead time
        suggestion.setConfidence(0.8);
        suggestion.setReasoning("Generated based on " + triggerReason + " trigger");
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(triggerReason);
        suggestion.setCreatedAt(LocalDateTime.now());
        
        return reorderSuggestionRepository.save(suggestion);
    }
    /**
     * Get pending suggestions for pricing and reorder
     */
    public List<PricingSuggestion> getPendingPricingSuggestions() {
        return pricingSuggestionRepository.findByStatus(SuggestionStatus.PENDING);
    }
    
    /**
     * Get a product by its ID
     */
    public Product getProductById(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        return productOpt.orElse(null);
    }
}