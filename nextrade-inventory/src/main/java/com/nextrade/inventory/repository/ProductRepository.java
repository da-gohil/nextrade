package com.nextrade.inventory.repository;

import com.nextrade.inventory.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> findActiveProducts(@Param("search") String search,
                                      @Param("categoryId") Long categoryId,
                                      Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity - p.reservedQuantity <= p.lowStockThreshold")
    java.util.List<Product> findLowStockProducts();
}
