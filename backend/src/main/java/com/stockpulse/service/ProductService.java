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
    
    @Autowired
    private CommerceAdvisorSelector commerceAdvisorSelector;
    
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
     * Evaluate triggers for low inventory or demand spikes using CommerceAdvisor
     */
    public void evaluateTriggers(Product product) {
        // Check for low inventory trigger
        if (product.getStockLevel() < product.getReorderThreshold()) {
            // Move to PRICE_REVIEW_PENDING if not already
            if (product.getStatus() != ProductStatus.PRICE_REVIEW_PENDING) {
                product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
                // Generate pricing suggestion using CommerceAdvisor
                generatePricingSuggestionWithAdvisor(product, TriggerReason.INVENTORY_LOW);
            }
        }
        
        // TODO: Check for demand spike trigger
        // For now, we'll skip this as we don't have demand thresholds defined
    }
    
    /**
     * Generate a pricing suggestion using the CommerceAdvisor
     */
    private PricingSuggestion generatePricingSuggestionWithAdvisor(Product product, TriggerReason triggerReason) {
        AdvisoryResult result = commerceAdvisorSelector.generateRecommendations(product);
        PricingSuggestion pricingSuggestion = result.getPricingSuggestion();
        pricingSuggestion.setTriggerReason(triggerReason);
        
        return pricingSuggestionRepository.save(pricingSuggestion);
    }
    
    /**
     * Generate a reorder suggestion using the CommerceAdvisor
     */
    private ReorderSuggestion generateReorderSuggestionWithAdvisor(Product product, TriggerReason triggerReason) {
        AdvisoryResult result = commerceAdvisorSelector.generateRecommendations(product);
        ReorderSuggestion reorderSuggestion = result.getReorderSuggestion();
        reorderSuggestion.setTriggerReason(triggerReason);
        
        return reorderSuggestionRepository.save(reorderSuggestion);
    }
    
    /**
     * Generate a manual pricing suggestion using the CommerceAdvisor
     */
    @Transactional
    public PricingSuggestion generateManualPricingSuggestion(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        AdvisoryResult result = commerceAdvisorSelector.generateRecommendations(product);
        PricingSuggestion pricingSuggestion = result.getPricingSuggestion();
        pricingSuggestion.setTriggerReason(TriggerReason.MANUAL);
        pricingSuggestion.setReasoning(pricingSuggestion.getReasoning() + " (Manually triggered)");
        
        return pricingSuggestionRepository.save(pricingSuggestion);
    }
    

    
    /**
     * Generate a manual reorder suggestion using the CommerceAdvisor
     */
    @Transactional
    public ReorderSuggestion generateManualReorderSuggestion(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        Product product = productOpt.get();
        AdvisoryResult result = commerceAdvisorSelector.generateRecommendations(product);
        ReorderSuggestion reorderSuggestion = result.getReorderSuggestion();
        reorderSuggestion.setTriggerReason(TriggerReason.MANUAL);
        reorderSuggestion.setReasoning(reorderSuggestion.getReasoning() + " (Manually triggered)");
        
        return reorderSuggestionRepository.save(reorderSuggestion);
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