package com.example.mini_marketplace.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Getter
@Setter
public class ReviewRequest {
    @NotNull
    private Long productId;

    @Min(1)
    @Max(5)
    private int rating;

    private String comment;
}
