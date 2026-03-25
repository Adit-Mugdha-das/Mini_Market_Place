package com.example.mini_marketplace.controller.api;

import com.example.mini_marketplace.dto.ReviewRequest;
import com.example.mini_marketplace.dto.ReviewResponseDto;
import com.example.mini_marketplace.entity.Review;
import com.example.mini_marketplace.repository.ReviewRepository;
import com.example.mini_marketplace.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<List<ReviewResponseDto>> getAllReviews() {
        return ResponseEntity.ok(reviewRepository.findAll().stream()
                .map(ReviewResponseDto::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDto> getReview(@PathVariable Long id) {
        return reviewRepository.findById(id)
                .map(ReviewResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<Void> createReview(@Valid @RequestBody ReviewRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.submitReview(userDetails.getUsername(), request.getProductId(), request.getRating(), request.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateReview(@PathVariable Long id,
                                             @Valid @RequestBody ReviewRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.editReview(userDetails.getUsername(), id, request.getRating(), request.getComment());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            reviewService.adminDeleteReview(id);
        } else {
            reviewService.deleteReview(userDetails.getUsername(), id);
        }
        return ResponseEntity.noContent().build();
    }
}
