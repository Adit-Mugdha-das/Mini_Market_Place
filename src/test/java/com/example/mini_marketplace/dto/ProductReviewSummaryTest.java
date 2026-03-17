package com.example.mini_marketplace.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// Unit tests for DTO ProductReviewSummary
@DisplayName("ProductReviewSummary DTO Unit Tests")
class ProductReviewSummaryTest {

    private ProductReviewSummary summary;

    @BeforeEach
    void setUp() {
        summary = new ProductReviewSummary();
    }

    // ─── default state ────────────────────────────────────────────────────────

    @Test
    @DisplayName("new ProductReviewSummary — starCounts array is initialised with 5 zeroes")
    void newSummary_starCounts_initialisedToFiveZeroes() {
        assertThat(summary.getStarCounts()).hasSize(5);
        assertThat(summary.getStarCounts()).containsOnly(0L);
    }

    @Test
    @DisplayName("new ProductReviewSummary — averageRating defaults to 0.0")
    void newSummary_averageRating_defaultsToZero() {
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("new ProductReviewSummary — totalReviews defaults to 0")
    void newSummary_totalReviews_defaultsToZero() {
        assertThat(summary.getTotalReviews()).isEqualTo(0L);
    }

    // ─── setters / getters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setAverageRating — round-trips correctly")
    void setAverageRating_roundTrips() {
        summary.setAverageRating(4.3);
        assertThat(summary.getAverageRating()).isEqualTo(4.3);
    }

    @Test
    @DisplayName("setTotalReviews — round-trips correctly")
    void setTotalReviews_roundTrips() {
        summary.setTotalReviews(27L);
        assertThat(summary.getTotalReviews()).isEqualTo(27L);
    }

    @Test
    @DisplayName("setStarCounts — stores and retrieves individual star bucket values")
    void setStarCounts_storesCorrectly() {
        long[] counts = {2L, 5L, 10L, 8L, 20L}; // 1-star … 5-star
        summary.setStarCounts(counts);

        assertThat(summary.getStarCounts()[0]).isEqualTo(2L);  // 1-star
        assertThat(summary.getStarCounts()[4]).isEqualTo(20L); // 5-star
    }

    // ─── ReviewView inner class ───────────────────────────────────────────────

    @Test
    @DisplayName("ReviewView — all setters and getters round-trip correctly")
    void reviewView_allFields_roundTrip() {
        LocalDateTime now = LocalDateTime.now();
        ProductReviewSummary.ReviewView view = new ProductReviewSummary.ReviewView();
        view.setId(99L);
        view.setBuyerUsername("alice");
        view.setProductId(10L);
        view.setProductName("Smart Watch");
        view.setRating(5);
        view.setComment("Excellent product!");
        view.setCreatedAt(now);

        assertThat(view.getId()).isEqualTo(99L);
        assertThat(view.getBuyerUsername()).isEqualTo("alice");
        assertThat(view.getProductId()).isEqualTo(10L);
        assertThat(view.getProductName()).isEqualTo("Smart Watch");
        assertThat(view.getRating()).isEqualTo(5);
        assertThat(view.getComment()).isEqualTo("Excellent product!");
        assertThat(view.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("setReviews — list of ReviewView stored and retrieved intact")
    void setReviews_storesListCorrectly() {
        ProductReviewSummary.ReviewView v1 = new ProductReviewSummary.ReviewView();
        v1.setId(1L);
        v1.setRating(4);

        ProductReviewSummary.ReviewView v2 = new ProductReviewSummary.ReviewView();
        v2.setId(2L);
        v2.setRating(5);

        summary.setReviews(List.of(v1, v2));

        assertThat(summary.getReviews()).hasSize(2);
        assertThat(summary.getReviews().get(0).getId()).isEqualTo(1L);
        assertThat(summary.getReviews().get(1).getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("starCounts — sum of all buckets equals totalReviews when set consistently")
    void starCounts_sumEqualsTotalReviews_whenConsistent() {
        long[] counts = {1L, 2L, 3L, 4L, 5L}; // sum = 15
        summary.setStarCounts(counts);
        summary.setTotalReviews(15L);

        long sum = 0;
        for (long c : summary.getStarCounts()) sum += c;

        assertThat(sum).isEqualTo(summary.getTotalReviews());
    }
}
