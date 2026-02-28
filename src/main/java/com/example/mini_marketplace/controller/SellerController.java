package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.dto.ProductRequest;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var metrics = sellerService.getDashboardMetrics(userDetails.getUsername());
        model.addAttribute("username",       userDetails.getUsername());
        model.addAttribute("totalProducts",  metrics.getTotalProducts());
        model.addAttribute("activeProducts", metrics.getActiveProducts());
        model.addAttribute("totalOrders",    metrics.getTotalOrders());
        model.addAttribute("totalRevenue",   metrics.getTotalRevenue());
        return "dashboard/seller";
    }

    // ─── Products ──────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("products",
                sellerService.getMyProducts(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "seller/products";
    }

    @GetMapping("/products/add")
    public String showAddForm(Model model) {
        model.addAttribute("productRequest", new ProductRequest());
        return "seller/product-form";
    }

    @PostMapping("/products/add")
    public String addProduct(@Valid @ModelAttribute("productRequest") ProductRequest req,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "seller/product-form";

        sellerService.addProduct(req, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        return "redirect:/seller/products";
    }

    @GetMapping("/products/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Product product = sellerService.getProductForEdit(id, userDetails.getUsername());
            ProductRequest req = new ProductRequest();
            req.setName(product.getName());
            req.setDescription(product.getDescription());
            req.setPrice(product.getPrice());
            req.setQuantity(product.getQuantity());
            model.addAttribute("productRequest", req);
            model.addAttribute("productId", id);
            return "seller/product-form";
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/products";
        }
    }

    @PostMapping("/products/edit/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productRequest") ProductRequest req,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            return "seller/product-form";
        }
        try {
            sellerService.updateProduct(id, req, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerService.deleteProduct(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Product removed.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    // ─── Orders ────────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String viewOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("orders",
                sellerService.getMyOrders(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "seller/orders";
    }

    @PostMapping("/orders/{id}/advance")
    public String advanceOrder(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            sellerService.advanceOrderStatus(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + id + " status updated successfully.");
        } catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/seller/orders";
    }
}
