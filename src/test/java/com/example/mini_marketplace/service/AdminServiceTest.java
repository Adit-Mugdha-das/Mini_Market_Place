package com.example.mini_marketplace.service;

import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.RoleRepository;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock private UserRepository    userRepository;
    @Mock private RoleRepository    roleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository   orderRepository;
    @Mock private AuditService      auditService;

    @InjectMocks
    private AdminService adminService;

    private User normalUser;
    private User adminUser;
    private Role buyerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        buyerRole = new Role(Role.RoleName.ROLE_BUYER);
        adminRole = new Role(Role.RoleName.ROLE_ADMIN);

        normalUser = new User();
        normalUser.setId(10L);
        normalUser.setUsername("buyer1");
        normalUser.setEmail("buyer@test.com");
        normalUser.setPassword("pass");
        normalUser.setEnabled(true);
        normalUser.setRoles(Set.of(buyerRole));

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("pass");
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(adminRole));
    }

    // ─── deleteUser ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser — successfully deletes a normal user")
    void deleteUser_success() {
        // Arrange: Mock the repository to return a normal user
        when(userRepository.findById(10L)).thenReturn(Optional.of(normalUser));

        // Act: Call the deleteUser method
        adminService.deleteUser(10L, "admin");

        // Assert: Verify delete was called and audit log was created
        verify(userRepository).delete(normalUser);
        verify(auditService).log(eq("admin"), any(), any(), eq(10L), anyString());
    }

    @Test
    @DisplayName("deleteUser — throws when trying to delete an admin account")
    void deleteUser_throws_whenTargetIsAdmin() {
        // Arrange: Mock the repository to return an admin user
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // Act & Assert: Verify that deleting an admin throws an exception
        assertThatThrownBy(() -> adminService.deleteUser(1L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete an Admin account");

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser — throws when user not found")
    void deleteUser_throws_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(99L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    // ─── toggleUserEnabled ──────────────────────────────────────────────────

    @Test
    @DisplayName("toggleUserEnabled — disables an active user")
    void toggleUserEnabled_disablesActiveUser() {
        // Arrange: Mock an active user
        when(userRepository.findById(10L)).thenReturn(Optional.of(normalUser));

        // Act: Toggle the user's status
        adminService.toggleUserEnabled(10L, "admin");

        // Assert: Verify user is disabled and saved
        assertThat(normalUser.isEnabled()).isFalse();
        verify(userRepository).save(normalUser);
    }

    @Test
    @DisplayName("toggleUserEnabled — re-enables a disabled user")
    void toggleUserEnabled_enablesDisabledUser() {
        normalUser.setEnabled(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(normalUser));

        adminService.toggleUserEnabled(10L, "admin");

        assertThat(normalUser.isEnabled()).isTrue();
        verify(userRepository).save(normalUser);
    }

    @Test
    @DisplayName("toggleUserEnabled — throws when target is admin account")
    void toggleUserEnabled_throws_whenTargetIsAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.toggleUserEnabled(1L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot deactivate an Admin account");

        verify(userRepository, never()).save(any());
    }

    // ─── changeUserRole ─────────────────────────────────────────────────────

    @Test
    @DisplayName("changeUserRole — changes buyer to seller")
    void changeUserRole_buyerToSeller() {
        Role sellerRole = new Role(Role.RoleName.ROLE_SELLER);
        when(userRepository.findById(10L)).thenReturn(Optional.of(normalUser));
        when(roleRepository.findByName(Role.RoleName.ROLE_SELLER)).thenReturn(Optional.of(sellerRole));

        adminService.changeUserRole(10L, "SELLER", "admin");

        assertThat(normalUser.getRoles()).containsExactly(sellerRole);
        verify(userRepository).save(normalUser);
    }

    @Test
    @DisplayName("changeUserRole — throws when target is admin account")
    void changeUserRole_throws_whenTargetIsAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.changeUserRole(1L, "SELLER", "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change role of an Admin account");

        verify(userRepository, never()).save(any());
    }

    // ─── deleteProduct ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProduct — soft-deletes product by setting active=false")
    void deleteProduct_setsActiveFalse() {
        Product product = new Product();
        product.setId(5L);
        product.setName("Some Product");
        product.setActive(true);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        adminService.deleteProduct(5L, "admin");

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
        verify(auditService).log(eq("admin"), any(), any(), eq(5L), anyString());
    }

    @Test
    @DisplayName("deleteProduct — throws when product not found")
    void deleteProduct_throws_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteProduct(99L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    // ─── overrideOrderStatus ────────────────────────────────────────────────

    @Test
    @DisplayName("overrideOrderStatus — changes order status to DELIVERED")
    void overrideOrderStatus_success() {
        Order order = new Order();
        order.setId(20L);
        order.setStatus(Order.Status.SHIPPED);
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        adminService.overrideOrderStatus(20L, "DELIVERED", "admin");

        assertThat(order.getStatus()).isEqualTo(Order.Status.DELIVERED);
        verify(orderRepository).save(order);
        verify(auditService).log(eq("admin"), any(), any(), eq(20L), anyString());
    }

    @Test
    @DisplayName("overrideOrderStatus — throws on invalid status string")
    void overrideOrderStatus_throws_whenInvalidStatus() {
        Order order = new Order();
        order.setId(21L);
        order.setStatus(Order.Status.PENDING);
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminService.overrideOrderStatus(21L, "INVALID", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    @DisplayName("overrideOrderStatus — throws when order not found")
    void overrideOrderStatus_throws_whenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.overrideOrderStatus(99L, "DELIVERED", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    // ─── getAllUsers / getAllOrders / getAllProducts ───────────────────────────

    @Test
    @DisplayName("getAllUsers — delegates to userRepository.findAll()")
    void getAllUsers_delegatesToRepository() {
        when(userRepository.findAll()).thenReturn(java.util.List.of(normalUser, adminUser));

        var result = adminService.getAllUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getDashboardMetrics — returns correct aggregated values")
    void getDashboardMetrics_returnsAggregated() {
        when(userRepository.count()).thenReturn(5L);
        when(productRepository.count()).thenReturn(10L);
        when(productRepository.countAllActive()).thenReturn(8L);
        when(orderRepository.count()).thenReturn(3L);
        when(orderRepository.getTotalRevenue()).thenReturn(new BigDecimal("999.99"));

        var metrics = adminService.getDashboardMetrics();

        assertThat(metrics.getTotalUsers()).isEqualTo(5L);
        assertThat(metrics.getTotalProducts()).isEqualTo(10L);
        assertThat(metrics.getActiveProducts()).isEqualTo(8L);
        assertThat(metrics.getTotalOrders()).isEqualTo(3L);
        assertThat(metrics.getTotalRevenue()).isEqualByComparingTo("999.99");
    }
}
