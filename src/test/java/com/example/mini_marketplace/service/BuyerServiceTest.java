package com.example.mini_marketplace.service;

import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.exception.InsufficientStockException;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Unit tests for Buyer service methods
@DisplayName("BuyerService Unit Tests")
class BuyerServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository   orderRepository;
    @Mock private UserRepository    userRepository;
    @Mock private AuditService      auditService;

    @InjectMocks
    private BuyerService buyerService;

    private User   buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);
        buyer.setUsername("buyer1");
        buyer.setFullName("Test Buyer");
        buyer.setEmail("buyer@test.com");
        buyer.setPassword("pass");

        product = new Product();
        product.setId(10L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("50.00"));
        product.setQuantity(20);
        product.setActive(true);

        User seller = new User();
        seller.setId(2L);
        seller.setUsername("seller1");
        product.setSeller(seller);
    }

    // ─── getProductById ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getProductById — returns product when active")
    void getProductById_returnsProduct_whenActive() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Product result = buyerService.getProductById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("getProductById — throws when product not found")
    void getProductById_throws_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buyerService.getProductById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("getProductById — throws when product is inactive")
    void getProductById_throws_whenInactive() {
        product.setActive(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> buyerService.getProductById(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer available");
    }

    // ─── placeOrder ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("placeOrder — successfully places order and deducts stock")
    void placeOrder_success_deductsStock() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Order savedOrder = new Order();
        savedOrder.setId(100L);
        savedOrder.setBuyer(buyer);
        savedOrder.setTotalAmount(new BigDecimal("100.00"));
        savedOrder.setStatus(Order.Status.PENDING);
        savedOrder.setPaymentMethod(Order.PaymentMethod.CREDIT_CARD);
        savedOrder.setPaymentReference("TXN-TEST-ABC123");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = buyerService.placeOrder("buyer1", 10L, 2, "CREDIT_CARD");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(product.getQuantity()).isEqualTo(18); // 20 - 2
        verify(productRepository).save(product);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("placeOrder — throws InsufficientStockException when out of stock")
    void placeOrder_throws_whenOutOfStock() {
        product.setQuantity(0);
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> buyerService.placeOrder("buyer1", 10L, 1, "CREDIT_CARD"))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("placeOrder — throws InsufficientStockException when quantity exceeds stock")
    void placeOrder_throws_whenQuantityExceedsStock() {
        product.setQuantity(3);
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> buyerService.placeOrder("buyer1", 10L, 5, "CREDIT_CARD"))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("placeOrder — throws when quantity is zero or negative")
    void placeOrder_throws_whenQuantityInvalid() {
        assertThatThrownBy(() -> buyerService.placeOrder("buyer1", 10L, 0, "CREDIT_CARD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be at least 1");
    }

    @Test
    @DisplayName("placeOrder — defaults to CASH_ON_DELIVERY for unknown payment method")
    void placeOrder_defaultsToCoD_whenUnknownPaymentMethod() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Order savedOrder = new Order();
        savedOrder.setId(101L);
        savedOrder.setBuyer(buyer);
        savedOrder.setTotalAmount(new BigDecimal("50.00"));
        savedOrder.setStatus(Order.Status.PENDING);
        savedOrder.setPaymentMethod(Order.PaymentMethod.CASH_ON_DELIVERY);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Capture the order passed to save
        Order result = buyerService.placeOrder("buyer1", 10L, 1, "INVALID_METHOD");

        assertThat(result.getPaymentMethod()).isEqualTo(Order.PaymentMethod.CASH_ON_DELIVERY);
    }

    // ─── cancelOrder ────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder — cancels PENDING order and restores stock")
    void cancelOrder_success_restoresStock() {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(3);

        Order order = new Order();
        order.setId(50L);
        order.setBuyer(buyer);
        order.setStatus(Order.Status.PENDING);
        order.getItems().add(item);

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        buyerService.cancelOrder(50L, "buyer1");

        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(product.getQuantity()).isEqualTo(23); // 20 + 3
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("cancelOrder — throws when order is not PENDING")
    void cancelOrder_throws_whenNotPending() {
        Order order = new Order();
        order.setId(51L);
        order.setBuyer(buyer);
        order.setStatus(Order.Status.SHIPPED);

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(51L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> buyerService.cancelOrder(51L, "buyer1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    @DisplayName("cancelOrder — throws when buyer does not own the order")
    void cancelOrder_throws_whenNotOwner() {
        User otherBuyer = new User();
        otherBuyer.setId(99L);
        otherBuyer.setUsername("other");

        Order order = new Order();
        order.setId(52L);
        order.setBuyer(otherBuyer);
        order.setStatus(Order.Status.PENDING);

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(52L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> buyerService.cancelOrder(52L, "buyer1"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("do not own");
    }

    // ─── getMyOrders ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyOrders — returns empty list when buyer has no orders")
    void getMyOrders_returnsEmptyList_whenNoOrders() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        var result = buyerService.getMyOrders("buyer1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMyOrders — returns mapped order views")
    void getMyOrders_returnsMappedViews() {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("50.00"));

        Order order = new Order();
        order.setId(60L);
        order.setBuyer(buyer);
        order.setStatus(Order.Status.DELIVERED);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setPaymentMethod(Order.PaymentMethod.BKASH);
        order.setPaymentReference("TXN-TEST-XYZ");
        order.getItems().add(item);

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        var result = buyerService.getMyOrders("buyer1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(60L);
        assertThat(result.get(0).getStatus()).isEqualTo(Order.Status.DELIVERED);
        assertThat(result.get(0).getPaymentMethod()).isEqualTo(Order.PaymentMethod.BKASH);
        assertThat(result.get(0).getPaymentReference()).isEqualTo("TXN-TEST-XYZ");
    }
}
