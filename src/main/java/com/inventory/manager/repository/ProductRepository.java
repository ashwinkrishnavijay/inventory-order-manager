package com.inventory.manager.repository;

import com.inventory.manager.entity.Product;
import com.inventory.manager.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN p.categories c WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:maxStock IS NULL OR p.stockQuantity <= :maxStock)")
    Page<Product> searchProducts(
            @Param("status") ProductStatus status,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("maxStock") Integer maxStock,
            Pageable pageable
    );

    Page<Product> findByStockQuantityLessThanEqual(Integer threshold, Pageable pageable);
}
