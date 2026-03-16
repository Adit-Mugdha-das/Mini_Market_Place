package com.example.mini_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class SellerAccountProfileDto {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private boolean enabled;
    private LocalDateTime joinDate;

    private long totalProducts;
    private long activeProducts;
    private long totalSales;
    private BigDecimal totalRevenue;
    private double averageRating;
    private long totalReviews;
}
