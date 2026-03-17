package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.ProductReviewSummary;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Review;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.ReviewRepository;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Unit tests for Review service methods
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock private ReviewRepository  reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository    userRepository;
    @Mock private OrderRepository   orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User    buyer;
    private Product product;
    private Review  review;

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
        product.setName("Great Product");
        product.setSeller(seller);

        review = new Review();
        review.setId(100L);
        review.setBuyer(buyer);
        review.setProduct(product);
        review.setRating(4);
        review.setComment("Really good!");
    }

    // ─── canReview ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("canReview — returns true when eligible (delivered + not yet reviewed)")
    void canReview_returnsTrue_whenEligible() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(false);
        when(orderRepository.hasBuyerDeliveredProduct(1L, 10L)).thenReturn(true);

        assertThat(reviewService.canReview("buyer1", 10L)).isTrue();
    }

    @Test
    @DisplayName("canReview — returns false when already reviewed")
    void canReview_returnsFalse_whenAlreadyReviewed() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(true);

        assertThat(reviewService.canReview("buyer1", 10L)).isFalse();
        verify(orderRepository, never()).hasBuyerDeliveredProduct(anyLong(), anyLong());
    }

    @Test
    @DisplayName("canReview — returns false when no delivered order exists")
    void canReview_returnsFalse_whenNoDeliveredOrder() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(false);
        when(orderRepository.hasBuyerDeliveredProduct(1L, 10L)).thenReturn(false);

        assertThat(reviewService.canReview("buyer1", 10L)).isFalse();
    }

    // ─── submitReview ────────────────────────────────────────────────────────

    @Test
    @DisplayName("submitReview — saves review with correct fields")
    void submitReview_savesReview() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(false);
        when(orderRepository.hasBuyerDeliveredProduct(1L, 10L)).thenReturn(true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        reviewService.submitReview("buyer1", 10L, 5, "Excellent!");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Excellent!");
        assertThat(saved.getBuyer()).isEqualTo(buyer);
        assertThat(saved.getProduct()).isEqualTo(product);
    }

    @Test
    @DisplayName("submitReview — throws when rating is out of range (0 or 6)")
    void submitReview_throws_whenRatingInvalid() {
        assertThatThrownBy(() -> reviewService.submitReview("buyer1", 10L, 0, "Bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be 1–5");

        assertThatThrownBy(() -> reviewService.submitReview("buyer1", 10L, 6, "Bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be 1–5");
    }

    @Test
    @DisplayName("submitReview — throws when already reviewed")
    void submitReview_throws_whenAlreadyReviewed() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.submitReview("buyer1", 10L, 4, "Nice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    @DisplayName("submitReview — throws when no delivered order")
    void submitReview_throws_whenNoDeliveredOrder() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(false);
        when(orderRepository.hasBuyerDeliveredProduct(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.submitReview("buyer1", 10L, 4, "Nice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED orders");
    }

    @Test
    @DisplayName("submitReview — trims whitespace from comment")
    void submitReview_trimsComment() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(false);
        when(orderRepository.hasBuyerDeliveredProduct(1L, 10L)).thenReturn(true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        reviewService.submitReview("buyer1", 10L, 3, "   Nice one!   ");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getComment()).isEqualTo("Nice one!");
    }

    // ─── editReview ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("editReview — updates rating and comment of own review")
    void editReview_updatesOwnReview() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        reviewService.editReview("buyer1", 100L, 2, "Changed my mind.");

        assertThat(review.getRating()).isEqualTo(2);
        assertThat(review.getComment()).isEqualTo("Changed my mind.");
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("editReview — throws when trying to edit another buyer's review")
    void editReview_throws_whenNotOwner() {
        User otherBuyer = new User();
        otherBuyer.setId(99L);
        otherBuyer.setUsername("other");
        review.setBuyer(otherBuyer);

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.editReview("buyer1", 100L, 3, "Hmm"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("only edit your own");
    }

    @Test
    @DisplayName("editReview — throws when rating is invalid")
    void editReview_throws_whenRatingInvalid() {
        assertThatThrownBy(() -> reviewService.editReview("buyer1", 100L, 0, "Bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be 1–5");
    }

    // ─── deleteReview ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteReview — buyer can delete their own review")
    void deleteReview_success_forOwner() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        reviewService.deleteReview("buyer1", 100L);

        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("deleteReview — throws when buyer tries to delete another's review")
    void deleteReview_throws_whenNotOwner() {
        User otherBuyer = new User();
        otherBuyer.setId(99L);
        review.setBuyer(otherBuyer);

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview("buyer1", 100L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("only delete your own");
    }

    // ─── adminDeleteReview ───────────────────────────────────────────────────

    @Test
    @DisplayName("adminDeleteReview — deletes any review regardless of owner")
    void adminDeleteReview_deletesAnyReview() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        reviewService.adminDeleteReview(100L);

        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("adminDeleteReview — throws when review not found")
    void adminDeleteReview_throws_whenNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.adminDeleteReview(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    // ─── getSummary ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSummary — calculates average rating and star counts correctly")
    void getSummary_calculatesCorrectly() {
        Review r1 = buildReview(1L, 5, "Great");
        Review r2 = buildReview(2L, 3, "Okay");
        Review r3 = buildReview(3L, 4, "Good");

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(r1, r2, r3));

        ProductReviewSummary summary = reviewService.getSummary(10L);

        assertThat(summary.getTotalReviews()).isEqualTo(3);
        // avg = (5+3+4)/3 = 4.0
        assertThat(summary.getAverageRating()).isEqualTo(4.0);
        assertThat(summary.getStarCounts()[4]).isEqualTo(1L); // 5-star count
        assertThat(summary.getStarCounts()[3]).isEqualTo(1L); // 4-star count
        assertThat(summary.getStarCounts()[2]).isEqualTo(1L); // 3-star count
    }

    @Test
    @DisplayName("getSummary — returns zero average when no reviews")
    void getSummary_zeroAverage_whenNoReviews() {
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        ProductReviewSummary summary = reviewService.getSummary(10L);

        assertThat(summary.getTotalReviews()).isEqualTo(0);
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
    }

    // ─── hasReviewed ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasReviewed — returns true when review exists")
    void hasReviewed_returnsTrue() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(true);

        assertThat(reviewService.hasReviewed("buyer1", 10L)).isTrue();
    }

    @Test
    @DisplayName("hasReviewed — returns false when no review")
    void hasReviewed_returnsFalse() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(reviewRepository.existsByProductIdAndBuyerId(10L, 1L)).thenReturn(false);

        assertThat(reviewService.hasReviewed("buyer1", 10L)).isFalse();
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private Review buildReview(Long id, int rating, String comment) {
        Review r = new Review();
        r.setId(id);
        r.setBuyer(buyer);
        r.setProduct(product);
        r.setRating(rating);
        r.setComment(comment);
        return r;
    }
}
