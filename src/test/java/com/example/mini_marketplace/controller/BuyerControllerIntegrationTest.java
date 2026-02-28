package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.entity.Category;
import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.CategoryRepository;
import com.example.mini_marketplace.repository.ProductRepository;
import com.example.mini_marketplace.repository.RoleRepository;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("BuyerController Integration Tests")
class BuyerControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String BUYER_USERNAME  = "buyer_test";
    private static final String SELLER_USERNAME = "seller_for_buyer_test";

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        Role buyerRole = roleRepository.findByName(Role.RoleName.ROLE_BUYER)
                .orElseGet(() -> roleRepository.save(new Role(Role.RoleName.ROLE_BUYER)));
        Role sellerRole = roleRepository.findByName(Role.RoleName.ROLE_SELLER)
                .orElseGet(() -> roleRepository.save(new Role(Role.RoleName.ROLE_SELLER)));

        if (!userRepository.existsByUsername(BUYER_USERNAME)) {
            User buyer = new User();
            buyer.setFullName("Test Buyer");
            buyer.setUsername(BUYER_USERNAME);
            buyer.setEmail("buyer_test@example.com");
            buyer.setPassword(passwordEncoder.encode("password123"));
            buyer.setRoles(Set.of(buyerRole));
            userRepository.save(buyer);
        }

        User seller;
        if (!userRepository.existsByUsername(SELLER_USERNAME)) {
            seller = new User();
            seller.setFullName("Seller For Buyer Test");
            seller.setUsername(SELLER_USERNAME);
            seller.setEmail("seller_for_buyer@example.com");
            seller.setPassword(passwordEncoder.encode("password123"));
            seller.setRoles(Set.of(sellerRole));
            seller = userRepository.save(seller);
        } else {
            seller = userRepository.findByUsername(SELLER_USERNAME).orElseThrow();
        }

        // Create an active product for buyer tests
        Product product = new Product();
        product.setName("Integration Test Product");
        product.setDescription("A product for integration tests");
        product.setPrice(BigDecimal.valueOf(19.99));
        product.setQuantity(100);
        product.setActive(true);
        product.setSeller(seller);
        savedProduct = productRepository.save(product);
    }

    // ─── GET /buyer/products ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/products — returns 200 and renders product list view")
    void getProducts_asBuyer_returns200() throws Exception {
        mockMvc.perform(get("/buyer/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/products"))
                .andExpect(model().attributeExists(
                        "products", "productPage", "categories",
                        "currentPage", "totalPages", "username"));
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/products?keyword=Integration — filters results by keyword")
    void getProducts_withKeyword_filtersResults() throws Exception {
        mockMvc.perform(get("/buyer/products")
                        .param("keyword", "Integration"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/products"))
                .andExpect(model().attribute("keyword", "Integration"));
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/products — supports price range filtering")
    void getProducts_withPriceRange_returns200() throws Exception {
        mockMvc.perform(get("/buyer/products")
                        .param("minPrice", "10.00")
                        .param("maxPrice", "50.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/products"));
    }

    @Test
    @DisplayName("GET /buyer/products — unauthenticated user is redirected to login")
    void getProducts_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/buyer/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @WithMockUser(username = "seller_user", roles = "SELLER")
    @DisplayName("GET /buyer/products — SELLER role gets 403 Forbidden")
    void getProducts_asSeller_returns403() throws Exception {
        mockMvc.perform(get("/buyer/products"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /buyer/products/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/products/{id} — valid product returns 200 and populates model")
    void getProductDetail_validId_returns200() throws Exception {
        mockMvc.perform(get("/buyer/products/" + savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/product-detail"))
                .andExpect(model().attributeExists(
                        "product", "username", "reviewSummary", "canReview", "hasReviewed"));
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/products/{id} — non-existent product redirects with error")
    void getProductDetail_invalidId_redirectsWithError() throws Exception {
        mockMvc.perform(get("/buyer/products/999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/products"));
    }

    // ─── GET /buyer/checkout ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/checkout — valid product and quantity renders checkout page")
    void getCheckout_validParams_returns200() throws Exception {
        mockMvc.perform(get("/buyer/checkout")
                        .param("productId", savedProduct.getId().toString())
                        .param("quantity",  "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/checkout"))
                .andExpect(model().attributeExists("product", "quantity", "subtotal", "username"));
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/checkout — quantity exceeding stock redirects with error")
    void getCheckout_quantityExceedsStock_redirectsWithError() throws Exception {
        mockMvc.perform(get("/buyer/checkout")
                        .param("productId", savedProduct.getId().toString())
                        .param("quantity",  "99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/products/" + savedProduct.getId()));
    }

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/checkout — zero quantity redirects with error")
    void getCheckout_zeroQuantity_redirectsWithError() throws Exception {
        mockMvc.perform(get("/buyer/checkout")
                        .param("productId", savedProduct.getId().toString())
                        .param("quantity",  "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/products/" + savedProduct.getId()));
    }

    // ─── POST /buyer/checkout/pay ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("POST /buyer/checkout/pay — valid order redirects to payment-success")
    void postCheckoutPay_validOrder_redirectsToPaymentSuccess() throws Exception {
        mockMvc.perform(post("/buyer/checkout/pay")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("productId",     savedProduct.getId().toString())
                        .param("quantity",      "1")
                        .param("paymentMethod", "CREDIT_CARD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/payment-success"));
    }

    // ─── GET /buyer/payment-success ───────────────────────────────────────────

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/payment-success — returns 200 and renders payment success view")
    void getPaymentSuccess_asBuyer_returns200() throws Exception {
        mockMvc.perform(get("/buyer/payment-success"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/payment-success"))
                .andExpect(model().attributeExists("username"));
    }

    // ─── GET /buyer/orders ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = BUYER_USERNAME, roles = "BUYER")
    @DisplayName("GET /buyer/orders — returns 200 and renders orders view")
    void getOrders_asBuyer_returns200() throws Exception {
        mockMvc.perform(get("/buyer/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/orders"))
                .andExpect(model().attributeExists("orders", "username"));
    }
}
