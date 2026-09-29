package com.stockpulse.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pricing_suggestions")
public class PricingSuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "current_price")
    private BigDecimal currentPrice;
    
    @Column(name = "recommended_price")
    private BigDecimal recommendedPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_direction")
    private ChangeDirection changeDirection;
    
    private Double confidence;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;
    
    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason")
    private TriggerReason triggerReason;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}