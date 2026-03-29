package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.ProductRequest;
import com.example.mini_marketplace.dto.SellerAccountProfileDto;
import com.example.mini_marketplace.dto.SellerDashboardMetrics;
import com.example.mini_marketplace.dto.SellerOrderView;
import com.example.mini_marketplace.dto.SellerProfileUpdateRequest;
import com.example.mini_marketplace.dto.SellerProfileDto;
import com.example.mini_marketplace.entity.AuditLog.ActionType;
import com.example.mini_marketplace.entity.AuditLog.EntityType;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.exception.ResourceNotFoundException;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.repository.OrderItemRepository;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.ReviewRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

// Service handling seller-specific operations like product management and order fulfillment.
@Service
@RequiredArgsConstructor
public class SellerService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final AuditService auditService;

    // ─── helpers ───────────────────────────────────────────────────────────────

    private User getUser(String username) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!seller.isEnabled()) {
            throw new IllegalStateException("Your seller account is deactivated. Contact admin to reactivate it.");
        }
        return seller;
    }

    private User getUserAllowDisabled(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private Product getOwnedProduct(Long productId, User seller) {
        return productRepository.findByIdAndSeller(productId, seller)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or access denied."));
    }

    // ─── product CRUD ──────────────────────────────────────────────────────────

    public List<Product> getMyProducts(String username) {
        return productRepository.findBySellerAndActiveTrueOrderByCreatedAtDesc(getUser(username));
    }

    @Transactional
    public void addProduct(ProductRequest req, String username) {
        User seller = getUser(username);
        Product p = new Product();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setImageUrl(req.getImageUrl());
        p.setPrice(req.getPrice());
        p.setQuantity(req.getQuantity());
        p.setSeller(seller);
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(p::setCategory);
        }
        productRepository.save(p);
        auditService.log(username, ActionType.CREATE_PRODUCT, EntityType.PRODUCT,
                p.getId(), "Created: " + p.getName());
    }

    public Product getProductForEdit(Long productId, String username) {
        return getOwnedProduct(productId, getUser(username));
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest req, String username) {
        Product p = getOwnedProduct(productId, getUser(username));
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setImageUrl(req.getImageUrl());
        p.setPrice(req.getPrice());
        p.setQuantity(req.getQuantity());
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(p::setCategory);
        } else {
            p.setCategory(null);
        }
        productRepository.save(p);
        auditService.log(username, ActionType.UPDATE_PRODUCT, EntityType.PRODUCT,
                productId, "Updated: " + p.getName());
    }

    @Transactional
    public void deleteProduct(Long productId, String username) {
        Product p = getOwnedProduct(productId, getUser(username));
        p.setActive(false);
        productRepository.save(p);
        auditService.log(username, ActionType.DELETE_PRODUCT, EntityType.PRODUCT,
                productId, "Soft-deleted: " + p.getName());
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
            view.setPaymentMethod(order.getPaymentMethod());
            view.setPaymentReference(order.getPaymentReference());

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

    // ─── seller dashboard metrics ──────────────────────────────────────────────

    public SellerDashboardMetrics getDashboardMetrics(String username) {
        User seller = getUser(username);
        long totalProducts  = productRepository.findBySellerOrderByCreatedAtDesc(seller).size();
        long activeProducts = productRepository.countBySellerAndActiveTrue(seller);
        long totalOrders    = orderRepository.countOrdersBySellerId(seller.getId());
        BigDecimal revenue  = orderRepository.getRevenueForSeller(seller.getId());
        return new SellerDashboardMetrics(totalProducts, activeProducts, totalOrders, revenue);
    }

    // ─── seller order status advancement ──────────────────────────────────────

    @Transactional
    public void advanceOrderStatus(Long orderId, String username) {
        User seller = getUser(username);

        List<OrderItem> myItems =
                orderItemRepository.findByOrderIdAndSellerId(orderId, seller.getId());
        if (myItems.isEmpty()) {
            throw new ResourceNotFoundException("Order not found or access denied.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

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
        auditService.log(username, ActionType.ADVANCE_ORDER_STATUS, EntityType.ORDER,
                orderId, "Status changed to " + next);
    }

    // ─── Public seller profile ─────────────────────────────────────────────────

    public SellerProfileDto getSellerProfile(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));

        List<Product> allProducts = productRepository.findBySellerOrderByCreatedAtDesc(seller);
        long totalProducts  = allProducts.size();
        long activeProducts = productRepository.countBySellerAndActiveTrue(seller);
        long totalSales     = orderRepository.countDeliveredOrdersBySellerId(sellerId);
        BigDecimal revenue  = orderRepository.getRevenueForSeller(sellerId);

        double avgRating  = reviewRepository.getAverageRatingForSeller(sellerId);
        long totalReviews = reviewRepository.countReviewsForSeller(sellerId);

        // Up to 6 recent active products for the showcase grid
        List<SellerProfileDto.ProductSnippet> recent = allProducts.stream()
                .filter(Product::isActive)
                .limit(6)
                .map(p -> new SellerProfileDto.ProductSnippet(
                        p.getId(), p.getName(), p.getImageUrl(), p.getPrice(), p.getQuantity(),
                        p.getCategory() != null ? p.getCategory().getName() : null))
                .collect(Collectors.toList());

        return new SellerProfileDto(
                seller.getId(), seller.getUsername(), seller.getFullName(), seller.getCreatedAt(),
                totalProducts, activeProducts, totalSales, revenue,
                Math.round(avgRating * 10.0) / 10.0, totalReviews, recent);
    }

    public SellerProfileDto getSellerProfileByUsername(String username) {
        User seller = getUser(username);
        return getSellerProfile(seller.getId());
    }

    public SellerAccountProfileDto getSellerAccountProfile(String username) {
        User seller = getUserAllowDisabled(username);
        SellerProfileDto stats = getSellerProfile(seller.getId());

        return new SellerAccountProfileDto(
                seller.getId(),
                seller.getUsername(),
                seller.getFullName(),
                seller.getEmail(),
                seller.getPhoneNumber(),
                seller.getAddress(),
            seller.isEnabled(),
                seller.getCreatedAt(),
                stats.getTotalProducts(),
                stats.getActiveProducts(),
                stats.getTotalSales(),
                stats.getTotalRevenue(),
                stats.getAverageRating(),
                stats.getTotalReviews()
        );
    }

    public SellerProfileUpdateRequest getSellerProfileUpdateRequest(String username) {
        User seller = getUserAllowDisabled(username);
        SellerProfileUpdateRequest request = new SellerProfileUpdateRequest();
        request.setFullName(seller.getFullName());
        request.setEmail(seller.getEmail());
        request.setPhoneNumber(seller.getPhoneNumber());
        request.setAddress(seller.getAddress());
        return request;
    }

    @Transactional
    public void updateOwnProfile(String username, SellerProfileUpdateRequest request) {
        User seller = getUser(username);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, seller.getId())) {
            throw new IllegalArgumentException("Email is already used by another account.");
        }

        seller.setFullName(request.getFullName().trim());
        seller.setEmail(normalizedEmail);
        seller.setPhoneNumber(nullIfBlank(request.getPhoneNumber()));
        seller.setAddress(nullIfBlank(request.getAddress()));
        userRepository.save(seller);
    }

    @Transactional
    public void deactivateOwnAccount(String username) {
        User seller = getUser(username);

        List<Product> products = productRepository.findBySellerOrderByCreatedAtDesc(seller);
        for (Product product : products) {
            product.setActive(false);
        }
        productRepository.saveAll(products);

        seller.setEnabled(false);
        seller.getRoles().clear();
        userRepository.save(seller);

        auditService.log(username, ActionType.DELETE_USER, EntityType.USER,
                seller.getId(), "Seller deactivated own account");
    }

    private String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}







