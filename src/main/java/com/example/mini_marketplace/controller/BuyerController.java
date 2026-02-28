package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.service.BuyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/buyer")
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerService buyerService;

    // ─── 1. Product list ───────────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("products", buyerService.getAllActiveProducts());
        model.addAttribute("username", userDetails.getUsername());
        return "buyer/products";
    }

    // ─── 2. Product detail ─────────────────────────────────────────────────────

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Product product = buyerService.getProductById(id);
            model.addAttribute("product", product);
            model.addAttribute("username", userDetails.getUsername());
            return "buyer/product-detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products";
        }
    }

    // ─── 3. Place order (POST from product detail page) ───────────────────────

    @PostMapping("/orders/place")
    public String placeOrder(@RequestParam Long productId,
                             @RequestParam int quantity,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            buyerService.placeOrder(userDetails.getUsername(), productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order placed successfully! 🎉");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products/" + productId;
        }
        return "redirect:/buyer/orders";
    }

    // ─── 4. My orders ──────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("orders", buyerService.getMyOrders(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "buyer/orders";
    }
}
