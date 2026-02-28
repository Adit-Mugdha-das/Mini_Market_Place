package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.exception.InsufficientStockException;
import com.example.mini_marketplace.service.BuyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    // ─── 1. Product list (paginated + sortable) ────────────────────────────────

    @GetMapping("/products")
    public String listProducts(@RequestParam(defaultValue = "0")    int page,
                               @RequestParam(defaultValue = "9")    int size,
                               @RequestParam(defaultValue = "createdAt") String sortBy,
                               @RequestParam(defaultValue = "desc") String dir,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        Page<Product> productPage = buyerService.getActiveProductsPaged(page, size, sortBy, dir);
        model.addAttribute("productPage", productPage);
        model.addAttribute("products",    productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  productPage.getTotalPages());
        model.addAttribute("sortBy",      sortBy);
        model.addAttribute("dir",         dir);
        model.addAttribute("username",    userDetails.getUsername());
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
            model.addAttribute("product",  product);
            model.addAttribute("username", userDetails.getUsername());
            return "buyer/product-detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products";
        }
    }

    // ─── 3. Place order ────────────────────────────────────────────────────────

    @PostMapping("/orders/place")
    public String placeOrder(@RequestParam Long productId,
                             @RequestParam int quantity,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            buyerService.placeOrder(userDetails.getUsername(), productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully! 🎉");
        } catch (InsufficientStockException e) {
            // Let the GlobalExceptionHandler render the stock-error page
            throw e;
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products/" + productId;
        }
        return "redirect:/buyer/orders";
    }

    // ─── 4. Cancel order (only PENDING) ────────────────────────────────────────

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

    // ─── 5. My orders ──────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("orders",   buyerService.getMyOrders(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "buyer/orders";
    }
}


