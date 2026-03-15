package com.example.mini_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class BuyerProfileDto {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDateTime joinDate;
    private long totalOrders;
}
