package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.BuyerProfileDto;
import com.example.mini_marketplace.dto.BuyerProfileUpdateRequest;
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
import com.example.mini_marketplace.repository.ReviewRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Service handling buyer-related operations such as placing orders and managing profile.
@Service
@RequiredArgsConstructor
public class BuyerService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final AuditService auditService;

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public void assertBuyerEnabled(String username) {
        User buyer = getUser(username);
        if (!buyer.isEnabled()) {
            throw new IllegalStateException("Your account is deactivated. You cannot place orders until an admin reactivates your account.");
        }
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

    // ─── 1b. Search products (keyword + price range + category + pagination) ─────

    /**
     * @param keyword    product name substring — null/blank means match all
     * @param minPrice   null = no lower bound
     * @param maxPrice   null = no upper bound
     * @param categoryId null = all categories
     */
    public Page<Product> searchProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                        Long categoryId,
                                        int page, int size, String sortBy, String dir) {
        String field = switch (sortBy) {
            case "price" -> "price";
            case "name"  -> "name";
            default      -> "createdAt";
        };
        Sort sort = "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending()
                                                 : Sort.by(field).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String likePattern = (keyword != null && !keyword.isBlank())
                ? "%" + keyword.trim() + "%"
                : "%";

        BigDecimal min = (minPrice != null) ? minPrice : BigDecimal.ZERO;
        BigDecimal max = (maxPrice != null) ? maxPrice : new BigDecimal("999999999");

        return productRepository.searchActive(likePattern, min, max, categoryId, pageable);
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
    public Order placeOrder(String username, Long productId, int quantity, String paymentMethodStr) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1.");

        User buyer = getUser(username);
        if (!buyer.isEnabled()) {
            throw new IllegalStateException("Your account is deactivated. You cannot place orders until an admin reactivates your account.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (!product.isActive()) throw new IllegalStateException("Product is no longer available.");
        if (product.getQuantity() <= 0)
            throw new InsufficientStockException(product.getName(), quantity, 0);
        if (product.getQuantity() < quantity)
            throw new InsufficientStockException(product.getName(), quantity, product.getQuantity());

        // Deduct stock
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        BigDecimal unitPrice = product.getPrice();
        BigDecimal total     = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Generate simulated payment reference
        String txnDate = LocalDate.now().toString().replace("-", "");
        String txnRef  = "TXN-" + txnDate + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Resolve payment method
        Order.PaymentMethod pm = Order.PaymentMethod.CASH_ON_DELIVERY;
        if (paymentMethodStr != null && !paymentMethodStr.isBlank()) {
            try { pm = Order.PaymentMethod.valueOf(paymentMethodStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Order order = new Order();
        order.setBuyer(buyer);
        order.setTotalAmount(total);
        order.setStatus(Order.Status.PENDING);
        order.setPaymentMethod(pm);
        order.setPaymentReference(txnRef);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        order.getItems().add(item);

        Order saved = orderRepository.save(order);
        auditService.log(username, ActionType.PLACE_ORDER, EntityType.ORDER,
                saved.getId(), "Bought " + quantity + "x " + product.getName() + " via " + pm);
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
            view.setPaymentMethod(order.getPaymentMethod());
            view.setPaymentReference(order.getPaymentReference());

            List<BuyerOrderView.ItemView> itemViews = order.getItems().stream()
                    .map(BuyerOrderView.ItemView::from)
                    .collect(Collectors.toList());
            view.setItems(itemViews);

            return view;
        }).collect(Collectors.toList());
    }

    public BuyerProfileDto getBuyerProfile(String username) {
        User buyer = getUser(username);
        long totalOrders = orderRepository.countByBuyerId(buyer.getId());
        return new BuyerProfileDto(
                buyer.getId(),
                buyer.getUsername(),
                buyer.getFullName(),
                buyer.getEmail(),
                buyer.getPhoneNumber(),
                buyer.getAddress(),
            buyer.isEnabled(),
                buyer.getCreatedAt(),
                totalOrders
        );
    }

    public BuyerProfileUpdateRequest getBuyerProfileUpdateRequest(String username) {
        User buyer = getUser(username);
        BuyerProfileUpdateRequest request = new BuyerProfileUpdateRequest();
        request.setFullName(buyer.getFullName());
        request.setEmail(buyer.getEmail());
        request.setPhoneNumber(buyer.getPhoneNumber());
        request.setAddress(buyer.getAddress());
        return request;
    }

    @Transactional
    public void updateProfile(String username, BuyerProfileUpdateRequest request) {
        User buyer = getUser(username);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, buyer.getId())) {
            throw new IllegalArgumentException("Email is already used by another account.");
        }

        buyer.setFullName(request.getFullName().trim());
        buyer.setEmail(normalizedEmail);
        buyer.setPhoneNumber(nullIfBlank(request.getPhoneNumber()));
        buyer.setAddress(nullIfBlank(request.getAddress()));

        userRepository.save(buyer);
    }

    @Transactional
    public void deleteAccount(String username) {
        User buyer = getUser(username);

        reviewRepository.deleteByBuyerId(buyer.getId());
        List<Order> buyerOrders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId());
        orderRepository.deleteAll(buyerOrders);

        buyer.getRoles().clear();
        userRepository.save(buyer);

        auditService.log(username, ActionType.DELETE_USER, EntityType.USER,
                buyer.getId(), "Buyer deleted own account");
        userRepository.delete(buyer);
    }

    private String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
