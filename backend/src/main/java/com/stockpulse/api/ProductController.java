package com.stockpulse.api;

import com.stockpulse.domain.*;
import com.stockpulse.service.ProductService;
import com.stockpulse.service.SuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private SuggestionService suggestionService;
    
    @Autowired
    private ProductRepository productRepository;

    // Modified to support filtering by status and category
    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category) {
        
        if (status != null && category != null) {
            // Filter by both status and category
            return productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            // Filter by status only
            return productRepository.findByStatus(status);
        } else if (category != null) {
            // Filter by category only
            return productRepository.findByCategory(category);
        } else {
            // No filters, return all products
            return productRepository.findAll();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productRepository.findById(id)
                .map(product -> ResponseEntity.ok().body(product))
                .orElse(ResponseEntity.notFound().build());
    }

    // Enhanced POST endpoint to create product with initial stock and price
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        // Ensure default values are set if not provided
        if (product.getDemandVelocity() == null) {
            product.setDemandVelocity(0);
        }
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setSku(productDetails.getSku());
                    product.setName(productDetails.getName());
                    product.setCategory(productDetails.getCategory());
                    product.setCurrentPrice(productDetails.getCurrentPrice());
                    product.setStockLevel(productDetails.getStockLevel());
                    product.setReorderThreshold(productDetails.getReorderThreshold());
                    product.setDemandVelocity(productDetails.getDemandVelocity());
                    product.setStatus(productDetails.getStatus());
                    Product updatedProduct = productRepository.save(product);
                    return ResponseEntity.ok(updatedProduct);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public List<Product> getProductsByStatus(@PathVariable ProductStatus status) {
        return productRepository.findByStatus(status);
    }
    
    // PATCH /products/{id}/stock: Set the stock level to the supplied value
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStockLevel(@PathVariable String id, @RequestBody Map<String, Integer> payload) {
        Integer stockLevel = payload.get("stockLevel");
        if (stockLevel == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Product updatedProduct = productService.updateStockLevel(id, stockLevel);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // POST /products/{id}/orders: Simulate a sale
    @PostMapping("/{id}/orders")
    public ResponseEntity<Product> recordSale(@PathVariable String id, @RequestBody Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Product updatedProduct = productService.recordSale(id, quantity);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // POST /products/{id}/suggest-pricing: On-demand pricing trigger
    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<PricingSuggestion> generatePricingSuggestion(@PathVariable String id) {
        try {
            PricingSuggestion suggestion = productService.generateManualPricingSuggestion(id);
            return ResponseEntity.ok(suggestion);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // POST /products/{id}/suggest-reorder: On-demand reorder trigger
    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<ReorderSuggestion> generateReorderSuggestion(@PathVariable String id) {
        try {
            ReorderSuggestion suggestion = productService.generateManualReorderSuggestion(id);
            return ResponseEntity.ok(suggestion);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}