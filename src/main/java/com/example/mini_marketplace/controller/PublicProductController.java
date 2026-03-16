package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.service.BuyerService;
import com.example.mini_marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PublicProductController {

    private final BuyerService buyerService;
    private final ReviewService reviewService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/products")
    public String listProducts(@RequestParam(defaultValue = "") String keyword,
                               @RequestParam(required = false) BigDecimal minPrice,
                               @RequestParam(required = false) BigDecimal maxPrice,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "9") int size,
                               @RequestParam(defaultValue = "createdAt") String sortBy,
                               @RequestParam(defaultValue = "desc") String dir,
                               Model model) {
        Page<Product> productPage = buyerService.searchProducts(
                keyword, minPrice, maxPrice, categoryId, page, size, sortBy, dir);

        Map<Long, com.example.mini_marketplace.dto.ProductReviewSummary> ratingMap = new HashMap<>();
        for (Product product : productPage.getContent()) {
            ratingMap.put(product.getId(), reviewService.getSummary(product.getId()));
        }

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("ratingMap", ratingMap);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("dir", dir);
        model.addAttribute("keyword", keyword);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("categoryId", categoryId);

        return "public/products";
    }

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Product product = buyerService.getProductById(id);
            model.addAttribute("product", product);
            model.addAttribute("reviewSummary", reviewService.getSummary(id));
            return "public/product-detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/products";
        }
    }
}
