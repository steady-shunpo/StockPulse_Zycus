package com.stockpulse.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvisoryResult {
    private PricingSuggestion pricingSuggestion;
    private ReorderSuggestion reorderSuggestion;
}