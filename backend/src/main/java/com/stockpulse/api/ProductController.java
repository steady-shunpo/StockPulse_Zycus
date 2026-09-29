package com.stockpulse.api;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductCategory;
import com.stockpulse.domain.ProductRepository;
import com.stockpulse.domain.ProductStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // Modified to support filtering by status and category
    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) ProductCategory category) {
        
        if (status != null && category != null) {
            // Filter by both status and category
            ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
            return productRepository.findByStatusAndCategory(productStatus, category);
        } else if (status != null) {
            // Filter by status only
            ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
            return productRepository.findByStatus(productStatus);
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
    public List<Product> getProductsByStatus(@PathVariable String status) {
        ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
        return productRepository.findByStatus(productStatus);
    }
    
}