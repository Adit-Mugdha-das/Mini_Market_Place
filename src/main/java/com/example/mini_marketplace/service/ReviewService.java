package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.ProductReviewSummary;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Review;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.exception.ResourceNotFoundException;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.ReviewRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ── Can review? (must have DELIVERED order, not yet reviewed) ─────────────
    public boolean canReview(String username, Long productId) {
        User buyer = getUser(username);
        if (reviewRepository.existsByProductIdAndBuyerId(productId, buyer.getId())) return false;
        return orderRepository.hasBuyerDeliveredProduct(buyer.getId(), productId);
    }

    public boolean hasReviewed(String username, Long productId) {
        User buyer = getUser(username);
        return reviewRepository.existsByProductIdAndBuyerId(productId, buyer.getId());
    }

    // ── Submit new review ─────────────────────────────────────────────────────
    @Transactional
    public void submitReview(String username, Long productId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1–5.");
        User buyer = getUser(username);
        if (reviewRepository.existsByProductIdAndBuyerId(productId, buyer.getId()))
            throw new IllegalStateException("You have already reviewed this product.");
        if (!orderRepository.hasBuyerDeliveredProduct(buyer.getId(), productId))
            throw new IllegalStateException("You can only review products from your DELIVERED orders.");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        Review review = new Review();
        review.setProduct(product);
        review.setBuyer(buyer);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);
        reviewRepository.save(review);
    }

    // ── Edit own review ───────────────────────────────────────────────────────
    @Transactional
    public void editReview(String username, Long reviewId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1–5.");
        User buyer = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        if (!review.getBuyer().getId().equals(buyer.getId()))
            throw new SecurityException("You can only edit your own review.");
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);
        reviewRepository.save(review);
    }

    // ── Delete own review (buyer) ─────────────────────────────────────────────
    @Transactional
    public void deleteReview(String username, Long reviewId) {
        User buyer = getUser(username);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        if (!review.getBuyer().getId().equals(buyer.getId()))
            throw new SecurityException("You can only delete your own review.");
        reviewRepository.delete(review);
    }

    // ── Admin: delete any review ──────────────────────────────────────────────
    @Transactional
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        reviewRepository.delete(review);
    }

    // ── Get buyer's own review for a product (for pre-filling edit form) ──────
    public Review getMyReview(String username, Long productId) {
        User buyer = getUser(username);
        return reviewRepository.findByProductIdAndBuyerId(productId, buyer.getId()).orElse(null);
    }

    // ── Get all reviews for products owned by a seller ────────────────────────
    public List<ProductReviewSummary.ReviewView> getReviewsForSeller(String sellerUsername) {
        User seller = getUser(sellerUsername);
        return reviewRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId())
                .stream().map(r -> {
                    ProductReviewSummary.ReviewView v = new ProductReviewSummary.ReviewView();
                    v.setId(r.getId());
                    v.setBuyerUsername(r.getBuyer().getUsername());
                    v.setProductId(r.getProduct().getId());
                    v.setProductName(r.getProduct().getName());
                    v.setRating(r.getRating());
                    v.setComment(r.getComment());
                    v.setCreatedAt(r.getCreatedAt());
                    return v;
                }).collect(Collectors.toList());
    }

    // ── Get all reviews (admin) ───────────────────────────────────────────────
    public List<ProductReviewSummary.ReviewView> getAllReviews() {
        return reviewRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream().map(r -> {
                    ProductReviewSummary.ReviewView v = new ProductReviewSummary.ReviewView();
                    v.setId(r.getId());
                    v.setBuyerUsername(r.getBuyer().getUsername());
                    v.setProductId(r.getProduct().getId());
                    v.setProductName(r.getProduct().getName());
                    v.setRating(r.getRating());
                    v.setComment(r.getComment());
                    v.setCreatedAt(r.getCreatedAt());
                    return v;
                }).collect(Collectors.toList());
    }

    // ── Summary (avg + bar chart + review list) ───────────────────────────────
    public ProductReviewSummary getSummary(Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        ProductReviewSummary summary = new ProductReviewSummary();
        summary.setTotalReviews(reviews.size());
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        summary.setAverageRating(Math.round(avg * 10.0) / 10.0);
        long[] counts = new long[5];
        for (Review r : reviews) counts[r.getRating() - 1]++;
        summary.setStarCounts(counts);
        List<ProductReviewSummary.ReviewView> views = reviews.stream().map(r -> {
            ProductReviewSummary.ReviewView v = new ProductReviewSummary.ReviewView();
            v.setId(r.getId());
            v.setBuyerUsername(r.getBuyer().getUsername());
            v.setProductId(r.getProduct().getId());
            v.setProductName(r.getProduct().getName());
            v.setRating(r.getRating());
            v.setComment(r.getComment());
            v.setCreatedAt(r.getCreatedAt());
            return v;
        }).collect(Collectors.toList());
        summary.setReviews(views);
        return summary;
    }
}

