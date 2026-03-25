package com.example.mini_marketplace.controller.api;

import com.example.mini_marketplace.dto.ProductRequest;
import com.example.mini_marketplace.dto.ProductResponseDto;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.service.BuyerService;
import com.example.mini_marketplace.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final BuyerService buyerService;
    private final SellerService sellerService;

    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> listProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String dir) {

        Page<Product> products = buyerService.searchProducts(
                keyword, minPrice, maxPrice, categoryId, page, size, sortBy, dir);
        return ResponseEntity.ok(products.map(ProductResponseDto::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        Product product = buyerService.getProductById(id);
        return ResponseEntity.ok(ProductResponseDto.from(product));
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Void> addProduct(@Valid @RequestBody ProductRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        sellerService.addProduct(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long id,
                                              @Valid @RequestBody ProductRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        sellerService.updateProduct(id, request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        sellerService.deleteProduct(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
