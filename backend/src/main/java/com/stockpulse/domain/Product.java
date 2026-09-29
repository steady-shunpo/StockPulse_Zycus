package com.stockpulse.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    

    
    @Id
    @Column(length = 36)
    private String id;
    
    private String sku;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private ProductCategory category;
    
    @Column(name = "current_price")
    private BigDecimal currentPrice;
    
    @Column(name = "stock_level")
    private Integer stockLevel;
    
    @Column(name = "reorder_threshold")
    private Integer reorderThreshold;
    
    @Column(name = "demand_velocity")
    private Integer demandVelocity;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    
    // Sprint 2 Extension Seams
    @Column(name = "cost_price")
    private BigDecimal costPrice;
    
    @Column(name = "margin_floor")
    private BigDecimal marginFloor;
    
    @Column(name = "supplier_id")
    private String supplierId;
}