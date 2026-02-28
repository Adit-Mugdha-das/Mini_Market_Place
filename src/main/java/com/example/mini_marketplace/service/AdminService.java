package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.AdminDashboardMetrics;
import com.example.mini_marketplace.entity.AuditLog.ActionType;
import com.example.mini_marketplace.entity.AuditLog.EntityType;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.RoleRepository;
import com.example.mini_marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    // ═══════════════════════════════════════════════════════
    //  1. USER MANAGEMENT
    // ═══════════════════════════════════════════════════════

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_ADMIN);
        if (isAdmin) throw new IllegalStateException("Cannot delete an Admin account.");
        String uname = user.getUsername();
        userRepository.delete(user);
        auditService.log(adminUsername, ActionType.DELETE_USER, EntityType.USER,
                userId, "Deleted user: " + uname);
    }

    @Transactional
    public void toggleUserEnabled(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_ADMIN);
        if (isAdmin) throw new IllegalStateException("Cannot deactivate an Admin account.");
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        auditService.log(adminUsername, ActionType.TOGGLE_USER_STATUS, EntityType.USER,
                userId, user.getUsername() + " -> " + (user.isEnabled() ? "ENABLED" : "DISABLED"));
    }

    @Transactional
    public void changeUserRole(Long userId, String newRole, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_ADMIN);
        if (isAdmin) throw new IllegalStateException("Cannot change role of an Admin account.");

        Role.RoleName roleName = "SELLER".equalsIgnoreCase(newRole)
                ? Role.RoleName.ROLE_SELLER : Role.RoleName.ROLE_BUYER;
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
        user.setRoles(Set.of(role));
        userRepository.save(user);
        auditService.log(adminUsername, ActionType.CHANGE_USER_ROLE, EntityType.USER,
                userId, user.getUsername() + " -> " + newRole.toUpperCase());
    }

    // ═══════════════════════════════════════════════════════
    //  2. PRODUCT MANAGEMENT
    // ═══════════════════════════════════════════════════════

    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteProduct(Long productId, String adminUsername) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        p.setActive(false);
        productRepository.save(p);
        auditService.log(adminUsername, ActionType.ADMIN_DELETE_PRODUCT, EntityType.PRODUCT,
                productId, "Admin removed: " + p.getName());
    }

    // ═══════════════════════════════════════════════════════
    //  3. ORDER MANAGEMENT
    // ═══════════════════════════════════════════════════════

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public AdminDashboardMetrics getDashboardMetrics() {
        long totalUsers     = userRepository.count();
        long totalProducts  = productRepository.count();
        long activeProducts = productRepository.countAllActive();
        long totalOrders    = orderRepository.count();
        var  revenue        = orderRepository.getTotalRevenue();
        return new AdminDashboardMetrics(totalUsers, totalProducts, activeProducts, totalOrders, revenue);
    }

    @Transactional
    public void overrideOrderStatus(Long orderId, String statusStr, String adminUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        Order.Status newStatus;
        try {
            newStatus = Order.Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }
        Order.Status oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        auditService.log(adminUsername, ActionType.OVERRIDE_ORDER_STATUS, EntityType.ORDER,
                orderId, "Admin override: " + oldStatus + " -> " + newStatus);
    }
}
