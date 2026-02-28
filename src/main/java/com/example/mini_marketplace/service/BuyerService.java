package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.BuyerOrderView;
import com.example.mini_marketplace.entity.AuditLog.ActionType;
import com.example.mini_marketplace.entity.AuditLog.EntityType;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.exception.InsufficientStockException;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final AuditService auditService;

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ─── 1. View all active products (paginated + sortable) ────────────────────

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    /**
     * Paginated product listing.
     *
     * @param page    0-based page index
     * @param size    items per page
     * @param sortBy  field name: "price", "name", or "createdAt"
     * @param dir     "asc" or "desc"
     */
    public Page<Product> getActiveProductsPaged(int page, int size, String sortBy, String dir) {
        String field = switch (sortBy) {
            case "price" -> "price";
            case "name"  -> "name";
            default      -> "createdAt";
        };
        Sort sort = "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending()
                                                 : Sort.by(field).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByActiveTrue(pageable);
    }

    // ─── 1b. Search products (keyword + price range + pagination) ─────────────

    /**
     * @param keyword   product name substring — null/blank means match all
     * @param minPrice  null = no lower bound
     * @param maxPrice  null = no upper bound
     */
    public Page<Product> searchProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                        int page, int size, String sortBy, String dir) {
        String field = switch (sortBy) {
            case "price" -> "price";
            case "name"  -> "name";
            default      -> "createdAt";
        };
        Sort sort = "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending()
                                                 : Sort.by(field).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Build a LIKE pattern — "%%" matches everything when no keyword given
        String likePattern = (keyword != null && !keyword.isBlank())
                ? "%" + keyword.trim() + "%"
                : "%";

        // Replace null price bounds with open-ended sentinel values
        BigDecimal min = (minPrice != null) ? minPrice : BigDecimal.ZERO;
        BigDecimal max = (maxPrice != null) ? maxPrice : new BigDecimal("999999999");

        return productRepository.searchActive(likePattern, min, max, pageable);
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

    // ─── 3. Place order with stock validation ──────────────────────────────────

    @Transactional
    public Order placeOrder(String username, Long productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        User buyer = getUser(username);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (!product.isActive()) {
            throw new IllegalStateException("Product is no longer available.");
        }

        // ── Stock validation with custom exception ──────────────────────────
        if (product.getQuantity() <= 0) {
            throw new InsufficientStockException(product.getName(), quantity, 0);
        }
        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getQuantity());
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
        Order saved = orderRepository.save(order);
        auditService.log(username, ActionType.PLACE_ORDER, EntityType.ORDER,
                saved.getId(), "Bought " + quantity + "x " + product.getName());
        return saved;
    }

    // ─── 4. Cancel order (only if PENDING) ────────────────────────────────────

    @Transactional
    public void cancelOrder(Long orderId, String username) {
        User buyer = getUser(username);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        // Ownership check
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new SecurityException("You do not own this order.");
        }

        // State transition: only PENDING can be cancelled by buyer
        if (order.getStatus() != Order.Status.PENDING) {
            throw new IllegalStateException(
                    "Order #" + orderId + " cannot be cancelled — current status is " + order.getStatus() + ". Only PENDING orders can be cancelled.");
        }

        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        auditService.log(username, ActionType.CANCEL_ORDER, EntityType.ORDER,
                orderId, "Cancelled by buyer — stock restored");
    }

    // ─── 5. View buyer's own orders ────────────────────────────────────────────

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
