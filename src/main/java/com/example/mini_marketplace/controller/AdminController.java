package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.service.AdminService;
import com.example.mini_marketplace.service.AuditService;
import com.example.mini_marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final ReviewService reviewService;

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var metrics = adminService.getDashboardMetrics();
        model.addAttribute("username",       userDetails.getUsername());
        model.addAttribute("totalUsers",     metrics.getTotalUsers());
        model.addAttribute("totalProducts",  metrics.getTotalProducts());
        model.addAttribute("activeProducts", metrics.getActiveProducts());
        model.addAttribute("totalOrders",    metrics.getTotalOrders());
        model.addAttribute("totalRevenue",   metrics.getTotalRevenue());
        return "dashboard/admin";
    }

    // ─── 1. User Management ────────────────────────────────────────────────────

    @GetMapping("/users")
    public String listUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("users",    adminService.getAllUsers());
        model.addAttribute("username", userDetails.getUsername());
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        try {
            adminService.deleteUser(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        try {
            adminService.toggleUserEnabled(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "User status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/change-role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        try {
            adminService.changeUserRole(id, role, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "Role changed to " + role + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ─── 2. Product Management ─────────────────────────────────────────────────

    @GetMapping("/products")
    public String listProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("products", adminService.getAllProducts());
        model.addAttribute("username", userDetails.getUsername());
        return "admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        try {
            adminService.deleteProduct(id, userDetails.getUsername());
            ra.addFlashAttribute("successMessage", "Product removed.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ─── 3. Order Management ───────────────────────────────────────────────────

    @GetMapping("/orders")
    public String listOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("orders",      adminService.getAllOrders());
        model.addAttribute("username",    userDetails.getUsername());
        model.addAttribute("allStatuses", com.example.mini_marketplace.entity.Order.Status.values());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String overrideStatus(@PathVariable Long id,
                                 @RequestParam String status,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes ra) {
        try {
            adminService.overrideOrderStatus(id, status, userDetails.getUsername());
            ra.addFlashAttribute("successMessage",
                    "Order #" + id + " status changed to " + status + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // ─── 4. Audit Log ──────────────────────────────────────────────────────────

    @GetMapping("/audit")
    public String auditLog(@RequestParam(defaultValue = "0")  int page,
                           @RequestParam(defaultValue = "20") int size,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        var logPage = auditService.getRecentLogs(page, size);
        model.addAttribute("logPage",     logPage);
        model.addAttribute("logs",        logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  logPage.getTotalPages());
        model.addAttribute("username",    userDetails.getUsername());
        return "admin/audit";
    }

    // ─── 5. Reviews Management ─────────────────────────────────────────────────

    @GetMapping("/reviews")
    public String listReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("reviews",  reviewService.getAllReviews());
        model.addAttribute("username", userDetails.getUsername());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, RedirectAttributes ra) {
        try {
            reviewService.adminDeleteReview(id);
            ra.addFlashAttribute("successMessage", "Review deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reviews";
    }
}

