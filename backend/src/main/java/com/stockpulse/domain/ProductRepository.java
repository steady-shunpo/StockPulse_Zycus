package com.stockpulse.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStockLevelLessThanEqual(Integer threshold);
    List<Product> findByDemandVelocityGreaterThan(Integer velocity);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(ProductCategory category);
    List<Product> findByStatusAndCategory(ProductStatus status, ProductCategory category);
    List<Product> findByStatusIn(List<ProductStatus> statuses);
}