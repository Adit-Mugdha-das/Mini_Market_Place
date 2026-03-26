package com.example.mini_marketplace.controller.api;

import com.example.mini_marketplace.dto.BuyerOrderView;
import com.example.mini_marketplace.dto.OrderRequest;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.service.BuyerService;
import com.example.mini_marketplace.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final BuyerService buyerService;
    private final SellerService sellerService;

    // 1. GET /api/orders (List own orders for buyer)
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<List<BuyerOrderView>> listOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(buyerService.getMyOrders(userDetails.getUsername()));
    }

    // 2. GET /api/orders/{id} (Get specific order)
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/{id}")
    public ResponseEntity<BuyerOrderView> getOrder(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(buyerService.getOrder(id, userDetails.getUsername()));
    }

    // 3. POST /api/orders (Place order)
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<BuyerOrderView> createOrder(@Valid @RequestBody OrderRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        Order order = buyerService.placeOrder(
            userDetails.getUsername(),
            request.getProductId(),
            request.getQuantity(),
            request.getPaymentMethod()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BuyerOrderView.from(order));
    }

    // 4. PUT /api/orders/{id} (Update status - Seller advances status)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        // Assuming Seller can advance status. No body needed for simple advance, or could require status/action.
        // Reusing existing logic: advance defines the transition.
        sellerService.advanceOrderStatus(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // 5. DELETE /api/orders/{id} (Cancel order - Buyer)
    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        buyerService.cancelOrder(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
