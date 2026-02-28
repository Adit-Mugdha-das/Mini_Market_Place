package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.exception.InsufficientStockException;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.service.BuyerService;
import com.example.mini_marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/buyer")
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerService buyerService;
    private final ReviewService reviewService;
    private final CategoryRepository categoryRepository;

    // ─── 1. Product list ───────────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(@RequestParam(defaultValue = "")     String keyword,
                               @RequestParam(required = false)      BigDecimal minPrice,
                               @RequestParam(required = false)      BigDecimal maxPrice,
                               @RequestParam(required = false)      Long categoryId,
                               @RequestParam(defaultValue = "0")    int page,
                               @RequestParam(defaultValue = "9")    int size,
                               @RequestParam(defaultValue = "createdAt") String sortBy,
                               @RequestParam(defaultValue = "desc") String dir,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        Page<Product> productPage = buyerService.searchProducts(
                keyword, minPrice, maxPrice, categoryId, page, size, sortBy, dir);

        java.util.Map<Long, com.example.mini_marketplace.dto.ProductReviewSummary> ratingMap =
                new java.util.HashMap<>();
        for (Product p : productPage.getContent()) {
            ratingMap.put(p.getId(), reviewService.getSummary(p.getId()));
        }

        model.addAttribute("productPage",  productPage);
        model.addAttribute("products",     productPage.getContent());
        model.addAttribute("ratingMap",    ratingMap);
        model.addAttribute("categories",   categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("currentPage",  page);
        model.addAttribute("totalPages",   productPage.getTotalPages());
        model.addAttribute("sortBy",       sortBy);
        model.addAttribute("dir",          dir);
        model.addAttribute("keyword",      keyword);
        model.addAttribute("minPrice",     minPrice);
        model.addAttribute("maxPrice",     maxPrice);
        model.addAttribute("categoryId",   categoryId);
        model.addAttribute("username",     userDetails.getUsername());
        return "buyer/products";
    }

    // ─── 2. Product detail (includes reviews) ─────────────────────────────────

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Product product = buyerService.getProductById(id);
            String username = userDetails.getUsername();
            model.addAttribute("product",     product);
            model.addAttribute("username",    username);
            model.addAttribute("reviewSummary", reviewService.getSummary(id));
            model.addAttribute("canReview",   reviewService.canReview(username, id));
            model.addAttribute("hasReviewed", reviewService.hasReviewed(username, id));
            model.addAttribute("myReview",    reviewService.getMyReview(username, id));
            return "buyer/product-detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products";
        }
    }

    // ─── 3. Checkout page ──────────────────────────────────────────────────────

    @GetMapping("/checkout")
    public String checkoutPage(@RequestParam Long productId,
                               @RequestParam int quantity,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Product product = buyerService.getProductById(productId);
            if (quantity < 1 || quantity > product.getQuantity()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid quantity. Available stock: " + product.getQuantity());
                return "redirect:/buyer/products/" + productId;
            }
            java.math.BigDecimal subtotal = product.getPrice()
                    .multiply(java.math.BigDecimal.valueOf(quantity));
            model.addAttribute("product",   product);
            model.addAttribute("quantity",  quantity);
            model.addAttribute("subtotal",  subtotal);
            model.addAttribute("username",  userDetails.getUsername());
            return "buyer/checkout";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products";
        }
    }

    // ─── 4. Process payment & place order ─────────────────────────────────────

    @PostMapping("/checkout/pay")
    public String processPayment(@RequestParam Long productId,
                                 @RequestParam int quantity,
                                 @RequestParam String paymentMethod,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            com.example.mini_marketplace.entity.Order order =
                    buyerService.placeOrder(userDetails.getUsername(), productId, quantity, paymentMethod);
            redirectAttributes.addFlashAttribute("orderId",          order.getId());
            redirectAttributes.addFlashAttribute("paymentReference", order.getPaymentReference());
            redirectAttributes.addFlashAttribute("paymentMethod",    order.getPaymentMethod().name());
            redirectAttributes.addFlashAttribute("totalAmount",      order.getTotalAmount());
            return "redirect:/buyer/payment-success";
        } catch (com.example.mini_marketplace.exception.InsufficientStockException e) {
            throw e;
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products/" + productId;
        }
    }

    // ─── 5. Payment success page ───────────────────────────────────────────────

    @GetMapping("/payment-success")
    public String paymentSuccess(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "buyer/payment-success";
    }

    // ─── 6. Cancel order ───────────────────────────────────────────────────────

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            buyerService.cancelOrder(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + id + " has been cancelled. Stock restored.");
        } catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/orders";
    }

    // ─── 7. My orders ──────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("orders",   buyerService.getMyOrders(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "buyer/orders";
    }

    // ─── 6. Submit review ──────────────────────────────────────────────────────

    @PostMapping("/products/{id}/review")
    public String submitReview(@PathVariable Long id,
                               @RequestParam int rating,
                               @RequestParam(required = false) String comment,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            reviewService.submitReview(userDetails.getUsername(), id, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thank you! Your review has been submitted. ⭐");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/products/" + id + "#reviews";
    }

    // ─── 7. Edit review ────────────────────────────────────────────────────────

    @PostMapping("/reviews/{reviewId}/edit")
    public String editReview(@PathVariable Long reviewId,
                             @RequestParam Long productId,
                             @RequestParam int rating,
                             @RequestParam(required = false) String comment,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            reviewService.editReview(userDetails.getUsername(), reviewId, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Review updated successfully. ⭐");
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/products/" + productId + "#reviews";
    }

    // ─── 8. Delete review ──────────────────────────────────────────────────────

    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long reviewId,
                               @RequestParam Long productId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(userDetails.getUsername(), reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "Review deleted.");
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/buyer/products/" + productId + "#reviews";
    }
}




