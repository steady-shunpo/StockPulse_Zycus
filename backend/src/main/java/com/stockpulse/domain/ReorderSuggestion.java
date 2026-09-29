package com.stockpulse.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reorder_suggestions")
public class ReorderSuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "current_stock")
    private Integer currentStock;
    
    @Column(name = "recommended_quantity")
    private Integer recommendedQuantity;
    
    @Column(name = "suggested_lead_time_days")
    private Integer suggestedLeadTimeDays;
    
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