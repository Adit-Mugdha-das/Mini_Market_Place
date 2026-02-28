package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Fetch only the items in a given order that belong to a specific seller
    @Query("""
            SELECT oi FROM OrderItem oi
            JOIN oi.product p
            WHERE oi.order.id = :orderId AND p.seller.id = :sellerId
            """)
    List<OrderItem> findByOrderIdAndSellerId(@Param("orderId") Long orderId,
                                             @Param("sellerId") Long sellerId);
}
