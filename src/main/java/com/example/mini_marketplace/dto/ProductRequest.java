package com.example.mini_marketplace.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Image URL is too long")
    private String imageUrl;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least $0.01")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed $999,999.99")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0,    message = "Quantity cannot be negative")
    @Max(value = 99999, message = "Quantity cannot exceed 99,999")
    private Integer quantity;
}


