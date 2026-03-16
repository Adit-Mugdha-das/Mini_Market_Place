package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByProductIdAndBuyerId(Long productId, Long buyerId);

    void deleteByBuyerId(Long buyerId);

    boolean existsByProductIdAndBuyerId(Long productId, Long buyerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRating(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);

    // All reviews for products belonging to a seller
    @Query("""
            SELECT r FROM Review r
            JOIN r.product p
            WHERE p.seller.id = :sellerId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findBySellerIdOrderByCreatedAtDesc(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r JOIN r.product p WHERE p.seller.id = :sellerId")
    Double getAverageRatingForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM Review r JOIN r.product p WHERE p.seller.id = :sellerId")
    Long countReviewsForSeller(@Param("sellerId") Long sellerId);
}

