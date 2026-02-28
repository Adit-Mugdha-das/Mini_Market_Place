package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem Entity Unit Tests")
class OrderItemEntityTest {

    private OrderItem orderItem;
    private Order     order;
    private Product   product;

    @BeforeEach
    void setUp() {
        User buyer = new User();
        buyer.setId(1L);
        buyer.setUsername("buyer1");

        User seller = new User();
        seller.setId(2L);
        seller.setUsername("seller1");

        product = new Product();
        product.setId(5L);
        product.setName("Gaming Chair");
        product.setPrice(new BigDecimal("299.99"));
        product.setSeller(seller);

        order = new Order();
        order.setId(20L);
        order.setBuyer(buyer);

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(3);
        orderItem.setUnitPrice(new BigDecimal("299.99"));
    }

    // ─── no-arg constructor / defaults ────────────────────────────────────────

    @Test
    @DisplayName("no-arg constructor — produces non-null object")
    void noArgConstructor_producesNonNull() {
        assertThat(new OrderItem()).isNotNull();
    }

    @Test
    @DisplayName("new OrderItem — all fields null by default")
    void newOrderItem_allFieldsNullByDefault() {
        OrderItem fresh = new OrderItem();
        assertThat(fresh.getId()).isNull();
        assertThat(fresh.getOrder()).isNull();
        assertThat(fresh.getProduct()).isNull();
        assertThat(fresh.getQuantity()).isNull();
        assertThat(fresh.getUnitPrice()).isNull();
    }

    // ─── setters / getters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setQuantity / getQuantity — round-trips correctly")
    void quantity_roundTrips() {
        orderItem.setQuantity(7);
        assertThat(orderItem.getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("setUnitPrice / getUnitPrice — stores BigDecimal with full precision")
    void unitPrice_roundTripsPrecisely() {
        orderItem.setUnitPrice(new BigDecimal("149.95"));
        assertThat(orderItem.getUnitPrice()).isEqualByComparingTo("149.95");
    }

    @Test
    @DisplayName("setOrder / getOrder — holds correct order reference")
    void setOrder_holdsOrderReference() {
        assertThat(orderItem.getOrder()).isSameAs(order);
        assertThat(orderItem.getOrder().getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("setProduct / getProduct — holds correct product reference")
    void setProduct_holdsProductReference() {
        assertThat(orderItem.getProduct()).isSameAs(product);
        assertThat(orderItem.getProduct().getName()).isEqualTo("Gaming Chair");
    }

    @Test
    @DisplayName("setId / getId — round-trips correctly")
    void id_roundTrips() {
        orderItem.setId(99L);
        assertThat(orderItem.getId()).isEqualTo(99L);
    }

    // ─── computed subtotal ────────────────────────────────────────────────────

    @Test
    @DisplayName("subtotal — quantity × unitPrice calculates correctly")
    void subtotal_calculatesCorrectly() {
        orderItem.setQuantity(3);
        orderItem.setUnitPrice(new BigDecimal("299.99"));

        BigDecimal subtotal = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        assertThat(subtotal).isEqualByComparingTo("899.97");
    }

    @Test
    @DisplayName("subtotal — quantity 1 gives subtotal equal to unit price")
    void subtotal_quantityOne_equalsUnitPrice() {
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(new BigDecimal("49.99"));

        BigDecimal subtotal = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        assertThat(subtotal).isEqualByComparingTo(orderItem.getUnitPrice());
    }

    // ─── reassignment ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("setQuantity — overwrites previously set quantity")
    void setQuantity_overwritesPreviousValue() {
        orderItem.setQuantity(5);
        orderItem.setQuantity(2);
        assertThat(orderItem.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("setUnitPrice — overwrites previously set price")
    void setUnitPrice_overwritesPreviousValue() {
        orderItem.setUnitPrice(new BigDecimal("100.00"));
        orderItem.setUnitPrice(new BigDecimal("75.00"));
        assertThat(orderItem.getUnitPrice()).isEqualByComparingTo("75.00");
    }
}
