package com.stockpulse.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {
    List<PricingSuggestion> findByStatus(SuggestionStatus status);
    List<PricingSuggestion> findByProductId(String productId);
}