package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.service.AdminService;
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

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var metrics = adminService.getDashboardMetrics();
        model.addAttribute("username",      userDetails.getUsername());
        model.addAttribute("totalUsers",    metrics.getTotalUsers());
        model.addAttribute("totalProducts", metrics.getTotalProducts());
        model.addAttribute("activeProducts",metrics.getActiveProducts());
        model.addAttribute("totalOrders",   metrics.getTotalOrders());
        model.addAttribute("totalRevenue",  metrics.getTotalRevenue());
        return "dashboard/admin";
    }

    // ─── 1. User Management ────────────────────────────────────────────────────

    @GetMapping("/users")
    public String listUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        model.addAttribute("username", userDetails.getUsername());
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteUser(id);
            ra.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.toggleUserEnabled(id);
            ra.addFlashAttribute("successMessage", "User status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/change-role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        try {
            adminService.changeUserRole(id, role);
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
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteProduct(id);
            ra.addFlashAttribute("successMessage", "Product removed.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ─── 3. Order Management ───────────────────────────────────────────────────

    @GetMapping("/orders")
    public String listOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("orders",   adminService.getAllOrders());
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("allStatuses", com.example.mini_marketplace.entity.Order.Status.values());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String overrideStatus(@PathVariable Long id,
                                 @RequestParam String status,
                                 RedirectAttributes ra) {
        try {
            adminService.overrideOrderStatus(id, status);
            ra.addFlashAttribute("successMessage",
                    "Order #" + id + " status changed to " + status + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
