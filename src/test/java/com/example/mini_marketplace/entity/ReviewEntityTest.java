package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

// Unit tests for Review entity model
@DisplayName("Review Entity Unit Tests")
class ReviewEntityTest {

    private Review review;
    private User   buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);
        buyer.setUsername("buyer1");

        User seller = new User();
        seller.setId(2L);
        seller.setUsername("seller1");

        product = new Product();
        product.setId(10L);
        product.setName("Wireless Headphones");
        product.setSeller(seller);

        review = new Review();
        review.setId(100L);
        review.setBuyer(buyer);
        review.setProduct(product);
        review.setRating(5);
        review.setComment("Absolutely love it!");
    }

    // ─── no-arg constructor / defaults ────────────────────────────────────────

    @Test
    @DisplayName("no-arg constructor — produces non-null object")
    void noArgConstructor_producesNonNull() {
        assertThat(new Review()).isNotNull();
    }

    @Test
    @DisplayName("new Review — all fields null by default")
    void newReview_allFieldsNullByDefault() {
        Review fresh = new Review();
        assertThat(fresh.getId()).isNull();
        assertThat(fresh.getBuyer()).isNull();
        assertThat(fresh.getProduct()).isNull();
        assertThat(fresh.getRating()).isNull();
        assertThat(fresh.getComment()).isNull();
    }

    // ─── setters / getters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setRating / getRating — stores rating 1 correctly")
    void rating_storesMinValue() {
        review.setRating(1);
        assertThat(review.getRating()).isEqualTo(1);
    }

    @Test
    @DisplayName("setRating / getRating — stores rating 5 correctly")
    void rating_storesMaxValue() {
        review.setRating(5);
        assertThat(review.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("setComment / getComment — round-trips correctly")
    void comment_roundTrips() {
        review.setComment("Great product, fast delivery!");
        assertThat(review.getComment()).isEqualTo("Great product, fast delivery!");
    }

    @Test
    @DisplayName("setComment — null comment is stored as null")
    void comment_storesNull() {
        review.setComment(null);
        assertThat(review.getComment()).isNull();
    }

    @Test
    @DisplayName("setBuyer — holds the correct buyer reference")
    void setBuyer_holdsReference() {
        assertThat(review.getBuyer()).isSameAs(buyer);
        assertThat(review.getBuyer().getUsername()).isEqualTo("buyer1");
    }

    @Test
    @DisplayName("setProduct — holds the correct product reference")
    void setProduct_holdsReference() {
        assertThat(review.getProduct()).isSameAs(product);
        assertThat(review.getProduct().getName()).isEqualTo("Wireless Headphones");
    }

    @Test
    @DisplayName("setId / getId — round-trips correctly")
    void id_roundTrips() {
        review.setId(999L);
        assertThat(review.getId()).isEqualTo(999L);
    }

    // ─── rating update ────────────────────────────────────────────────────────

    @Test
    @DisplayName("setRating — overwrites previously set rating")
    void setRating_overwritesPreviousValue() {
        review.setRating(5);
        review.setRating(2);
        assertThat(review.getRating()).isEqualTo(2);
    }

    @Test
    @DisplayName("setComment — overwrites previously set comment")
    void setComment_overwritesPreviousValue() {
        review.setComment("First impression");
        review.setComment("Changed my mind after use");
        assertThat(review.getComment()).isEqualTo("Changed my mind after use");
    }
}
