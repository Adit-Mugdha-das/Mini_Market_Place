package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.dto.ProductRequest;
import com.example.mini_marketplace.dto.SellerProfileUpdateRequest;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.service.ImageUploadService;
import com.example.mini_marketplace.service.ReviewService;
import com.example.mini_marketplace.service.SellerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final ImageUploadService imageUploadService;
    private final ReviewService reviewService;
    private final CategoryRepository categoryRepository;

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

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public String myProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("profile", sellerService.getSellerAccountProfile(username));
        model.addAttribute("profileForm", sellerService.getSellerProfileUpdateRequest(username));
        return "seller/my-profile";
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("profileForm") SellerProfileUpdateRequest profileForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            model.addAttribute("username", username);
            model.addAttribute("profile", sellerService.getSellerAccountProfile(username));
            return "seller/my-profile";
        }

        try {
            sellerService.updateOwnProfile(username, profileForm);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/seller/profile";
    }

    @PostMapping("/profile/delete")
    @PreAuthorize("hasRole('SELLER')")
    public String deleteProfile(@AuthenticationPrincipal UserDetails userDetails,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerService.deactivateOwnAccount(userDetails.getUsername());
            new SecurityContextLogoutHandler().logout(request, response, null);
            session.invalidate();
            redirectAttributes.addFlashAttribute("successMessage", "Your seller account has been deleted.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to delete account: " + e.getMessage());
            return "redirect:/seller/profile";
        }
    }

    // ─── Products ──────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("products", sellerService.getMyProducts(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "seller/products";
    }

    @GetMapping("/products/add")
    public String showAddForm(Model model) {
        model.addAttribute("productRequest", new ProductRequest());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        return "seller/product-form";
    }

    @PostMapping("/products/add")
    public String addProduct(@Valid @ModelAttribute("productRequest") ProductRequest req,
                             BindingResult bindingResult,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "seller/product-form";
        try {
            // File upload takes priority over URL if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                req.setImageUrl(imageUploadService.save(imageFile));
            }
            sellerService.addProduct(req, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Product listed successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload image: " + e.getMessage());
        }
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
            req.setImageUrl(product.getImageUrl());
            req.setPrice(product.getPrice());
            req.setQuantity(product.getQuantity());
            if (product.getCategory() != null) req.setCategoryId(product.getCategory().getId());
            model.addAttribute("productRequest", req);
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
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
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
            return "seller/product-form";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                req.setImageUrl(imageUploadService.save(imageFile));
            }
            sellerService.updateProduct(id, req, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload image: " + e.getMessage());
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
        model.addAttribute("orders", sellerService.getMyOrders(userDetails.getUsername()));
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

    // ─── Reviews ───────────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public String viewReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("reviews", reviewService.getReviewsForSeller(userDetails.getUsername()));
        model.addAttribute("username", userDetails.getUsername());
        return "seller/reviews";
    }
}
