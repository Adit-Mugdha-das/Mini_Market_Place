package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product Entity Unit Tests")
class ProductEntityTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Mechanical Keyboard");
        product.setDescription("RGB backlit, 60% layout");
        product.setPrice(new BigDecimal("89.99"));
        product.setQuantity(100);
    }

    // ─── default state ────────────────────────────────────────────────────────

    @Test
    @DisplayName("new Product — isActive defaults to true")
    void newProduct_isActiveByDefault() {
        Product fresh = new Product();
        assertThat(fresh.isActive()).isTrue();
    }

    @Test
    @DisplayName("new Product — no-arg constructor produces non-null object")
    void noArgConstructor_producesNonNullObject() {
        assertThat(new Product()).isNotNull();
    }

    // ─── getters / setters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setName / getName — round-trips correctly")
    void name_roundTrips() {
        product.setName("Gaming Mouse");
        assertThat(product.getName()).isEqualTo("Gaming Mouse");
    }

    @Test
    @DisplayName("setPrice / getPrice — stores BigDecimal precisely")
    void price_roundTrips() {
        product.setPrice(new BigDecimal("1234.56"));
        assertThat(product.getPrice()).isEqualByComparingTo("1234.56");
    }

    @Test
    @DisplayName("setQuantity / getQuantity — updates correctly")
    void quantity_roundTrips() {
        product.setQuantity(42);
        assertThat(product.getQuantity()).isEqualTo(42);
    }

    @Test
    @DisplayName("setActive(false) — marks product as inactive")
    void setActive_false_marksInactive() {
        product.setActive(false);
        assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("setActive(true) — re-activates a deactivated product")
    void setActive_true_reactivatesProduct() {
        product.setActive(false);
        product.setActive(true);
        assertThat(product.isActive()).isTrue();
    }

    // ─── seller / category assignment ─────────────────────────────────────────

    @Test
    @DisplayName("setSeller — product holds reference to assigned seller")
    void setSeller_holdsSellerReference() {
        User seller = new User();
        seller.setId(5L);
        seller.setUsername("seller1");
        product.setSeller(seller);

        assertThat(product.getSeller()).isSameAs(seller);
        assertThat(product.getSeller().getUsername()).isEqualTo("seller1");
    }

    @Test
    @DisplayName("setCategory — product holds reference to assigned category")
    void setCategory_holdsCategoryReference() {
        Category cat = new Category();
        cat.setId(3L);
        cat.setName("Electronics");
        product.setCategory(cat);

        assertThat(product.getCategory()).isSameAs(cat);
        assertThat(product.getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("setImageUrl — stores and retrieves image URL")
    void setImageUrl_storesUrl() {
        product.setImageUrl("https://cdn.example.com/kb.jpg");
        assertThat(product.getImageUrl()).isEqualTo("https://cdn.example.com/kb.jpg");
    }

    @Test
    @DisplayName("setDescription — stores and retrieves description")
    void setDescription_storesDescription() {
        product.setDescription("Compact tenkeyless design");
        assertThat(product.getDescription()).isEqualTo("Compact tenkeyless design");
    }
}
