package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.ProductRequest;
import com.example.mini_marketplace.entity.Order;
import com.example.mini_marketplace.entity.OrderItem;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.repository.OrderItemRepository;
import com.example.mini_marketplace.repository.OrderRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.ReviewRepository;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("SellerService Unit Tests")
class SellerServiceTest {

    @Mock private ProductRepository  productRepository;
    @Mock private OrderRepository    orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository     userRepository;
    @Mock private CategoryRepository categoryRepository;
    @SuppressWarnings("unused") // required by @InjectMocks — SellerService depends on it
    @Mock private ReviewRepository   reviewRepository;
    @Mock private AuditService       auditService;

    @InjectMocks
    private SellerService sellerService;

    private User    seller;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller1");
        seller.setFullName("Best Seller");
        seller.setEmail("seller@test.com");

        product = new Product();
        product.setId(10L);
        product.setName("Widget");
        product.setPrice(new BigDecimal("29.99"));
        product.setQuantity(50);
        product.setActive(true);
        product.setSeller(seller);
    }

    // ─── addProduct ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addProduct — saves product with all fields from request")
    void addProduct_savesProductCorrectly() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        ProductRequest req = new ProductRequest();
        req.setName("New Widget");
        req.setDescription("A great widget");
        req.setPrice(new BigDecimal("19.99"));
        req.setQuantity(100);
        req.setImageUrl("http://img.com/widget.jpg");
        req.setCategoryId(null);

        sellerService.addProduct(req, "seller1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("New Widget");
        assertThat(saved.getDescription()).isEqualTo("A great widget");
        assertThat(saved.getPrice()).isEqualByComparingTo("19.99");
        assertThat(saved.getQuantity()).isEqualTo(100);
        assertThat(saved.getSeller()).isEqualTo(seller);
        assertThat(saved.getImageUrl()).isEqualTo("http://img.com/widget.jpg");
        verify(auditService).log(eq("seller1"), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("addProduct — sets category when categoryId is provided")
    void addProduct_setsCategory_whenCategoryIdProvided() {
        com.example.mini_marketplace.entity.Category cat = new com.example.mini_marketplace.entity.Category();
        cat.setId(5L);
        cat.setName("Electronics");

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));

        ProductRequest req = new ProductRequest();
        req.setName("Gadget");
        req.setPrice(new BigDecimal("99.00"));
        req.setQuantity(10);
        req.setCategoryId(5L);

        sellerService.addProduct(req, "seller1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(cat);
    }

    // ─── deleteProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProduct — soft-deletes product (sets active=false)")
    void deleteProduct_softDeletesProduct() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(productRepository.findByIdAndSeller(10L, seller)).thenReturn(Optional.of(product));

        sellerService.deleteProduct(10L, "seller1");

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
        verify(auditService).log(eq("seller1"), any(), any(), eq(10L), anyString());
    }

    @Test
    @DisplayName("deleteProduct — throws SecurityException when seller doesn't own the product")
    void deleteProduct_throws_whenNotOwner() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(productRepository.findByIdAndSeller(10L, seller)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.deleteProduct(10L, "seller1"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("access denied");

        verify(productRepository, never()).save(any());
    }

    // ─── updateProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct — updates all fields on owned product")
    void updateProduct_updatesFields() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(productRepository.findByIdAndSeller(10L, seller)).thenReturn(Optional.of(product));

        ProductRequest req = new ProductRequest();
        req.setName("Updated Widget");
        req.setDescription("Updated desc");
        req.setPrice(new BigDecimal("39.99"));
        req.setQuantity(25);
        req.setImageUrl("http://img.com/updated.jpg");
        req.setCategoryId(null);

        sellerService.updateProduct(10L, req, "seller1");

        assertThat(product.getName()).isEqualTo("Updated Widget");
        assertThat(product.getPrice()).isEqualByComparingTo("39.99");
        assertThat(product.getQuantity()).isEqualTo(25);
        assertThat(product.getCategory()).isNull();
        verify(productRepository).save(product);
    }

    // ─── advanceOrderStatus ───────────────────────────────────────────────────

    @Test
    @DisplayName("advanceOrderStatus — advances PENDING → CONFIRMED")
    void advanceOrderStatus_pendingToConfirmed() {
        Order order = new Order();
        order.setId(20L);
        order.setStatus(Order.Status.PENDING);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByOrderIdAndSellerId(20L, 1L)).thenReturn(List.of(item));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));

        sellerService.advanceOrderStatus(20L, "seller1");

        assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("advanceOrderStatus — advances CONFIRMED → SHIPPED")
    void advanceOrderStatus_confirmedToShipped() {
        Order order = new Order();
        order.setId(21L);
        order.setStatus(Order.Status.CONFIRMED);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByOrderIdAndSellerId(21L, 1L)).thenReturn(List.of(item));
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));

        sellerService.advanceOrderStatus(21L, "seller1");

        assertThat(order.getStatus()).isEqualTo(Order.Status.SHIPPED);
    }

    @Test
    @DisplayName("advanceOrderStatus — advances SHIPPED → DELIVERED")
    void advanceOrderStatus_shippedToDelivered() {
        Order order = new Order();
        order.setId(22L);
        order.setStatus(Order.Status.SHIPPED);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByOrderIdAndSellerId(22L, 1L)).thenReturn(List.of(item));
        when(orderRepository.findById(22L)).thenReturn(Optional.of(order));

        sellerService.advanceOrderStatus(22L, "seller1");

        assertThat(order.getStatus()).isEqualTo(Order.Status.DELIVERED);
    }

    @Test
    @DisplayName("advanceOrderStatus — throws when order is already DELIVERED")
    void advanceOrderStatus_throws_whenAlreadyDelivered() {
        Order order = new Order();
        order.setId(23L);
        order.setStatus(Order.Status.DELIVERED);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByOrderIdAndSellerId(23L, 1L)).thenReturn(List.of(item));
        when(orderRepository.findById(23L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> sellerService.advanceOrderStatus(23L, "seller1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already DELIVERED");
    }

    @Test
    @DisplayName("advanceOrderStatus — throws when seller has no items in the order")
    void advanceOrderStatus_throws_whenSellerHasNoItems() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByOrderIdAndSellerId(99L, 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> sellerService.advanceOrderStatus(99L, "seller1"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("access denied");
    }

    @Test
    @DisplayName("advanceOrderStatus — throws when order is CANCELLED")
    void advanceOrderStatus_throws_whenCancelled() {
        Order order = new Order();
        order.setId(24L);
        order.setStatus(Order.Status.CANCELLED);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByOrderIdAndSellerId(24L, 1L)).thenReturn(List.of(item));
        when(orderRepository.findById(24L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> sellerService.advanceOrderStatus(24L, "seller1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    // ─── getMyProducts ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyProducts — returns products belonging to seller")
    void getMyProducts_returnsSellerProducts() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(productRepository.findBySellerOrderByCreatedAtDesc(seller))
                .thenReturn(List.of(product));

        List<Product> result = sellerService.getMyProducts("seller1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Widget");
    }

    @Test
    @DisplayName("getMyProducts — returns empty list when seller has no products")
    void getMyProducts_returnsEmpty_whenNoProducts() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(productRepository.findBySellerOrderByCreatedAtDesc(seller)).thenReturn(List.of());

        assertThat(sellerService.getMyProducts("seller1")).isEmpty();
    }
}
