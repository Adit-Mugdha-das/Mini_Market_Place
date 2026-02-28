package com.example.mini_marketplace.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class ProductReviewSummary {
    private double averageRating;
    private long totalReviews;
    private List<ReviewView> reviews;
    /** index 0 = 1-star count … index 4 = 5-star count */
    private long[] starCounts = new long[5];

    @Getter @Setter
    public static class ReviewView {
        private Long id;
        private String buyerUsername;
        private Long productId;
        private String productName;
        private int rating;
        private String comment;
        private LocalDateTime createdAt;
    }
}
