package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller/profile")
@RequiredArgsConstructor
public class PublicSellerController {

    private final SellerService sellerService;

    @GetMapping("/{sellerId}")
    public String viewProfile(@PathVariable Long sellerId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("profile",  sellerService.getSellerProfile(sellerId));
            model.addAttribute("username", userDetails.getUsername());
            return "seller/profile";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/products";
        }
    }
}
