package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find all orders that contain at least one product belonging to this seller
    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN o.items i
            JOIN i.product p
            WHERE p.seller.id = :sellerId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findOrdersBySellerId(@Param("sellerId") Long sellerId);

    // Find all orders placed by a specific buyer
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    // Admin: all orders newest first
    List<Order> findAllByOrderByCreatedAtDesc();

    // ── Metrics ──────────────────────────────────────────────────────────────

    // Platform-wide total revenue (exclude CANCELLED)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> 'CANCELLED'")
    BigDecimal getTotalRevenue();

    // Count of distinct orders that include a specific seller's products
    @Query("""
            SELECT COUNT(DISTINCT o) FROM Order o
            JOIN o.items i
            JOIN i.product p
            WHERE p.seller.id = :sellerId
            """)
    long countOrdersBySellerId(@Param("sellerId") Long sellerId);

    // Seller revenue: sum of (unitPrice * quantity) for items belonging to this seller (exclude CANCELLED)
    @Query("""
            SELECT COALESCE(SUM(oi.unitPrice * oi.quantity), 0)
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.product p
            WHERE p.seller.id = :sellerId
              AND o.status <> 'CANCELLED'
            """)
    BigDecimal getRevenueForSeller(@Param("sellerId") Long sellerId);
}


