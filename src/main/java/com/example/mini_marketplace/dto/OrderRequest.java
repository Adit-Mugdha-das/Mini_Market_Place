package com.example.mini_marketplace.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Getter
@Setter
public class OrderRequest {
    @NotNull
    private Long productId;
    
    @Min(1)
    private int quantity;
    
    @NotNull
    private String paymentMethod;
}
