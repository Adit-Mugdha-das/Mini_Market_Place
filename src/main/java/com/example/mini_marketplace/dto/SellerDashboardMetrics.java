package com.example.mini_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SellerDashboardMetrics {
    private final long totalProducts;       // all (including inactive)
    private final long activeProducts;
    private final long totalOrders;
    private final BigDecimal totalRevenue;  // excludes CANCELLED orders
}
