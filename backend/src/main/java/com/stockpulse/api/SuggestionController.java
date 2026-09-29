package com.stockpulse.api;

import com.stockpulse.domain.*;
import com.stockpulse.service.SuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class SuggestionController {
    
    @Autowired
    private SuggestionService suggestionService;
    
    // PATCH /pricing-suggestions/{id}: Update pricing suggestion status
    @PatchMapping("/pricing-suggestions/{id}")
    public ResponseEntity<PricingSuggestion> updatePricingSuggestionStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> payload) {
        
        String statusStr = payload.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            SuggestionStatus status = SuggestionStatus.valueOf(statusStr.toUpperCase());
            PricingSuggestion updatedSuggestion = suggestionService.updatePricingSuggestionStatus(id, status);
            return ResponseEntity.ok(updatedSuggestion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // PATCH /reorder-suggestions/{id}: Update reorder suggestion status
    @PatchMapping("/reorder-suggestions/{id}")
    public ResponseEntity<ReorderSuggestion> updateReorderSuggestionStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> payload) {
        
        String statusStr = payload.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            SuggestionStatus status = SuggestionStatus.valueOf(statusStr.toUpperCase());
            ReorderSuggestion updatedSuggestion = suggestionService.updateReorderSuggestionStatus(id, status);
            return ResponseEntity.ok(updatedSuggestion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // GET /suggestions/pending: Convenience query for frontend dashboard polling
    @GetMapping("/suggestions/pending")
    public ResponseEntity<Map<String, Object>> getPendingSuggestions() {
        List<PricingSuggestion> pendingPricing = suggestionService.getPendingPricingSuggestions();
        List<ReorderSuggestion> pendingReorder = suggestionService.getPendingReorderSuggestions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("pricingSuggestions", pendingPricing);
        response.put("reorderSuggestions", pendingReorder);
        
        return ResponseEntity.ok(response);
    }
}