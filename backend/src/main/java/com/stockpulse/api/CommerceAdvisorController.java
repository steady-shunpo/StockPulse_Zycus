package com.stockpulse.api;

import com.stockpulse.domain.AdvisoryResult;
import com.stockpulse.domain.Product;
import com.stockpulse.service.AdvisoryPersistenceService;
import com.stockpulse.service.CommerceAdvisorSelector;
import com.stockpulse.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commerce-advisor")
@CrossOrigin(origins = "http://localhost:5173")
public class CommerceAdvisorController {
    
    @Autowired
    private CommerceAdvisorSelector commerceAdvisorSelector;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private AdvisoryPersistenceService advisoryPersistenceService;
    
    /**
     * Generate pricing and reorder recommendations for a specific product
     */
    @PostMapping("/recommendations/{productId}")
    public ResponseEntity<AdvisoryResult> generateRecommendations(@PathVariable String productId) {
        try {
            Product product = productService.getProductById(productId);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }
            
            AdvisoryResult result = commerceAdvisorSelector.generateRecommendations(product);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Generate and persist pricing and reorder recommendations for a specific product
     */
    @PostMapping("/recommendations/{productId}/persist")
    public ResponseEntity<AdvisoryResult> generateAndPersistRecommendations(@PathVariable String productId) {
        try {
            Product product = productService.getProductById(productId);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }
            
            AdvisoryResult result = commerceAdvisorSelector.generateRecommendations(product);
            
            // Persist the suggestions
            advisoryPersistenceService.persistAdvisoryResult(result);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get the currently active advisor strategy
     */
    @GetMapping("/strategy")
    public ResponseEntity<String> getActiveStrategy() {
        return ResponseEntity.ok(commerceAdvisorSelector.getActiveStrategy());
    }
}