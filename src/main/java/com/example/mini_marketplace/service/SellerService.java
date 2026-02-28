package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.ProductRequest;
import com.example.mini_marketplace.dto.SellerOrderView;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.OrderItemRepository;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    // ─── helpers ───────────────────────────────────────────────────────────────

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private Product getOwnedProduct(Long productId, User seller) {
        return productRepository.findByIdAndSeller(productId, seller)
                .orElseThrow(() -> new SecurityException("Product not found or access denied."));
    }

    // ─── product CRUD ──────────────────────────────────────────────────────────

    public List<Product> getMyProducts(String username) {
        return productRepository.findBySellerOrderByCreatedAtDesc(getUser(username));
    }

    @Transactional
    public void addProduct(ProductRequest req, String username) {
        User seller = getUser(username);
        Product p = new Product();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setQuantity(req.getQuantity());
        p.setSeller(seller);
        productRepository.save(p);
    }

    public Product getProductForEdit(Long productId, String username) {
        return getOwnedProduct(productId, getUser(username));
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest req, String username) {
        Product p = getOwnedProduct(productId, getUser(username));
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setQuantity(req.getQuantity());
        productRepository.save(p);
    }

    @Transactional
    public void deleteProduct(Long productId, String username) {
        Product p = getOwnedProduct(productId, getUser(username));
        p.setActive(false);          // soft-delete so existing orders stay intact
        productRepository.save(p);
    }

    // ─── seller orders view ────────────────────────────────────────────────────

    public List<SellerOrderView> getMyOrders(String username) {
        User seller = getUser(username);
        List<Order> orders = orderRepository.findOrdersBySellerId(seller.getId());

        return orders.stream().map(order -> {
            List<OrderItem> myItems =
                    orderItemRepository.findByOrderIdAndSellerId(order.getId(), seller.getId());

            SellerOrderView view = new SellerOrderView();
            view.setOrderId(order.getId());
            view.setBuyerUsername(order.getBuyer().getUsername());
            view.setStatus(order.getStatus());
            view.setCreatedAt(order.getCreatedAt());

            List<SellerOrderView.ItemView> itemViews = myItems.stream()
                    .map(SellerOrderView.ItemView::from)
                    .collect(Collectors.toList());
            view.setSellerItems(itemViews);

            BigDecimal total = itemViews.stream()
                    .map(SellerOrderView.ItemView::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            view.setSellerTotal(total);

            return view;
        }).collect(Collectors.toList());
    }

    // ─── seller order status advancement ──────────────────────────────────────
    // PENDING → CONFIRMED → SHIPPED → DELIVERED

    @Transactional
    public void advanceOrderStatus(Long orderId, String username) {
        User seller = getUser(username);

        // Verify this seller has items in this order
        List<OrderItem> myItems =
                orderItemRepository.findByOrderIdAndSellerId(orderId, seller.getId());
        if (myItems.isEmpty()) {
            throw new SecurityException("Order not found or access denied.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        Order.Status next = switch (order.getStatus()) {
            case PENDING   -> Order.Status.CONFIRMED;
            case CONFIRMED -> Order.Status.SHIPPED;
            case SHIPPED   -> Order.Status.DELIVERED;
            case DELIVERED -> throw new IllegalStateException(
                    "Order #" + orderId + " is already DELIVERED.");
            case CANCELLED -> throw new IllegalStateException(
                    "Order #" + orderId + " is CANCELLED and cannot be advanced.");
        };

        order.setStatus(next);
        orderRepository.save(order);
    }
}
