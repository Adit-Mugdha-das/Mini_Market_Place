package com.example.mini_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SellerProfileDto {

    private Long id;
    private String username;
    private String fullName;
    private LocalDateTime joinDate;

    // Stats
    private long totalProducts;
    private long activeProducts;
    private long totalSales;         // distinct delivered orders
    private BigDecimal totalRevenue;
    private double averageRating;
    private long totalReviews;

    // Recent active products (max 6 for the profile showcase)
    private List<ProductSnippet> recentProducts;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ProductSnippet {
        private Long id;
        private String name;
        private String imageUrl;
        private java.math.BigDecimal price;
        private int quantity;
        private String categoryName;
    }
}
