package com.example.mini_marketplace.dto;

import com.example.mini_marketplace.entity.Review;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewResponseDto {
    private Long id;
    private Long productId;
    private Long buyerId;
    private String buyerUsername;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponseDto from(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());

        if (review.getProduct() != null) {
            dto.setProductId(review.getProduct().getId());
        }

        if (review.getBuyer() != null) {
            dto.setBuyerId(review.getBuyer().getId());
            dto.setBuyerUsername(review.getBuyer().getUsername());
        }
        return dto;
    }
}
