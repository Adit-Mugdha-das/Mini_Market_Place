package com.example.mini_marketplace.controller;

import com.example.mini_marketplace.entity.Product;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
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
// Integration tests for SellerController endpoints
@DisplayName("SellerController Integration Tests")
class SellerControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String SELLER_USERNAME = "seller_test";

    @BeforeEach
    void setUp() {
        Role sellerRole = roleRepository.findByName(Role.RoleName.ROLE_SELLER)
                .orElseGet(() -> roleRepository.save(new Role(Role.RoleName.ROLE_SELLER)));

        if (!userRepository.existsByUsername(SELLER_USERNAME)) {
            User seller = new User();
            seller.setFullName("Test Seller");
            seller.setUsername(SELLER_USERNAME);
            seller.setEmail("seller_test@example.com");
            seller.setPassword(passwordEncoder.encode("password123"));
            seller.setRoles(Set.of(sellerRole));
            userRepository.save(seller);
        }
    }

    // ─── GET /seller/dashboard ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("GET /seller/dashboard — returns 200 and renders seller dashboard view")
    void getDashboard_asSeller_returns200() throws Exception {
        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/seller"))
                .andExpect(model().attributeExists("username", "totalProducts",
                        "activeProducts", "totalOrders", "totalRevenue"));
    }

    @Test
    @DisplayName("GET /seller/dashboard — unauthenticated user is redirected to login")
    void getDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    // ─── GET /seller/products ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("GET /seller/products — returns 200 and renders product list view")
    void getProducts_asSeller_returns200() throws Exception {
        mockMvc.perform(get("/seller/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/products"))
                .andExpect(model().attributeExists("products", "username"));
    }

    @Test
    @WithMockUser(username = "buyer_user", roles = "BUYER")
    @DisplayName("GET /seller/products — BUYER role gets 403 Forbidden")
    void getProducts_asBuyer_returns403() throws Exception {
        mockMvc.perform(get("/seller/products"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /seller/products/add ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("GET /seller/products/add — returns 200 and puts productRequest and categories in model")
    void getAddProductForm_asSeller_returns200() throws Exception {
        mockMvc.perform(get("/seller/products/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().attributeExists("productRequest", "categories"));
    }

    // ─── POST /seller/products/add ────────────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("POST /seller/products/add — valid data redirects to product list with success")
    void postAddProduct_validData_redirectsToProductList() throws Exception {
        mockMvc.perform(post("/seller/products/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name",        "Test Widget")
                        .param("description", "A fine widget")
                        .param("price",       "29.99")
                        .param("quantity",    "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));
    }

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("POST /seller/products/add — blank fields stay on form with validation errors")
    void postAddProduct_blankFields_staysOnFormWithErrors() throws Exception {
        mockMvc.perform(post("/seller/products/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name",     "")
                        .param("price",    "")
                        .param("quantity", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().hasErrors());
    }

    // ─── GET /seller/products/edit/{id} ───────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("GET /seller/products/edit/{id} — own product returns 200 and renders edit form")
    void getEditForm_ownProduct_returns200() throws Exception {
        User seller = userRepository.findByUsername(SELLER_USERNAME).orElseThrow();
        Product product = new Product();
        product.setName("My Product");
        product.setDescription("desc");
        product.setPrice(BigDecimal.valueOf(9.99));
        product.setQuantity(10);
        product.setSeller(seller);
        product = productRepository.save(product);

        mockMvc.perform(get("/seller/products/edit/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/product-form"))
                .andExpect(model().attributeExists("productRequest", "productId", "categories"));
    }

    // ─── POST /seller/products/delete/{id} ────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("POST /seller/products/delete/{id} — own product is deleted and redirects to product list")
    void deleteProduct_ownProduct_redirectsToProductList() throws Exception {
        User seller = userRepository.findByUsername(SELLER_USERNAME).orElseThrow();
        Product product = new Product();
        product.setName("To Be Deleted");
        product.setDescription("desc");
        product.setPrice(BigDecimal.valueOf(1.00));
        product.setQuantity(1);
        product.setSeller(seller);
        product = productRepository.save(product);

        mockMvc.perform(post("/seller/products/delete/" + product.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/products"));
    }

    // ─── GET /seller/orders ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("GET /seller/orders — returns 200 and renders orders view")
    void getOrders_asSeller_returns200() throws Exception {
        mockMvc.perform(get("/seller/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/orders"))
                .andExpect(model().attributeExists("orders", "username"));
    }

    // ─── GET /seller/reviews ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = SELLER_USERNAME, roles = "SELLER")
    @DisplayName("GET /seller/reviews — returns 200 and renders reviews view")
    void getReviews_asSeller_returns200() throws Exception {
        mockMvc.perform(get("/seller/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/reviews"))
                .andExpect(model().attributeExists("reviews", "username"));
    }
}
