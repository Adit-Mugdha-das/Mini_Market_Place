package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
