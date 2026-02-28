package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.BuyerOrderView;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
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
public class BuyerService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ─── 1. View all active products ───────────────────────────────────────────

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    // ─── 2. View a single product ──────────────────────────────────────────────

    public Product getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        if (!p.isActive()) {
            throw new IllegalArgumentException("Product is no longer available.");
        }
        return p;
    }

    // ─── 3. Place order directly ───────────────────────────────────────────────

    @Transactional
    public Order placeOrder(String username, Long productId, int quantity) {
        User buyer = getUser(username);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (!product.isActive()) {
            throw new IllegalStateException("Product is no longer available.");
        }
        if (product.getQuantity() < quantity) {
            throw new IllegalStateException("Not enough stock. Available: " + product.getQuantity());
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        // Deduct stock
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        // Build order
        BigDecimal unitPrice = product.getPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Order order = new Order();
        order.setBuyer(buyer);
        order.setTotalAmount(total);
        order.setStatus(Order.Status.PENDING);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);

        order.getItems().add(item);
        return orderRepository.save(order);
    }

    // ─── 4. View buyer's own orders ────────────────────────────────────────────

    public List<BuyerOrderView> getMyOrders(String username) {
        User buyer = getUser(username);
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId());

        return orders.stream().map(order -> {
            BuyerOrderView view = new BuyerOrderView();
            view.setOrderId(order.getId());
            view.setStatus(order.getStatus());
            view.setCreatedAt(order.getCreatedAt());
            view.setTotalAmount(order.getTotalAmount());

            List<BuyerOrderView.ItemView> itemViews = order.getItems().stream()
                    .map(BuyerOrderView.ItemView::from)
                    .collect(Collectors.toList());
            view.setItems(itemViews);

            return view;
        }).collect(Collectors.toList());
    }
}
