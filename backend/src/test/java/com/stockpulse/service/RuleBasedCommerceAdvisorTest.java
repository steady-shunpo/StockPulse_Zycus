package com.stockpulse.service;

import com.stockpulse.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleBasedCommerceAdvisorTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private RuleBasedCommerceAdvisor advisor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testGenerateRecommendations_ReturnsAdvisoryResult() {
        // Arrange
        Product product = new Product();
        product.setId("test-product");
        product.setCategory(ProductCategory.ELECTRONICS);
        product.setCurrentPrice(new BigDecimal("100.00"));
        product.setStockLevel(5);
        product.setReorderThreshold(10);
        product.setDemandVelocity(20);
        
        // Mock category products for average calculation
        Product otherProduct = new Product();
        otherProduct.setDemandVelocity(30);
        List<Product> categoryProducts = Arrays.asList(product, otherProduct);
        when(productRepository.findByCategory(ProductCategory.ELECTRONICS)).thenReturn(categoryProducts);
        
        // Act
        AdvisoryResult result = advisor.generateRecommendations(product);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getPricingSuggestion());
        assertNotNull(result.getReorderSuggestion());
    }
}