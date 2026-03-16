package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerOrderByCreatedAtDesc(User seller);

    Optional<Product> findByIdAndSeller(Long id, User seller);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.seller.enabled = true")
    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.seller.enabled = true")
    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findAllByOrderByCreatedAtDesc();

    // ── Search: keyword + price range + optional category ────────────────────
    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND p.seller.enabled = true
              AND LOWER(p.name) LIKE LOWER(:keyword)
              AND p.price >= :minPrice
              AND p.price <= :maxPrice
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            """)
    Page<Product> searchActive(@Param("keyword")    String keyword,
                               @Param("minPrice")   BigDecimal minPrice,
                               @Param("maxPrice")   BigDecimal maxPrice,
                               @Param("categoryId") Long categoryId,
                               Pageable pageable);

    // ── Metrics ──────────────────────────────────────────────────────────────
    long countBySellerAndActiveTrue(User seller);

    @Query("SELECT COUNT(DISTINCT p) FROM Product p WHERE p.active = true AND p.seller.enabled = true")
    long countAllActive();
}






