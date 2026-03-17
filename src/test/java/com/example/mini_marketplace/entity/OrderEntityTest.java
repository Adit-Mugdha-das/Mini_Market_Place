package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

// Unit tests for Order entity model
@DisplayName("Order Entity Unit Tests")
class OrderEntityTest {

    private Order order;
    private User  buyer;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);
        buyer.setUsername("buyer1");

        order = new Order();
        order.setId(10L);
        order.setBuyer(buyer);
    }

    // ─── defaults ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("new Order — status defaults to PENDING")
    void newOrder_status_defaultsPending() {
        assertThat(new Order().getStatus()).isEqualTo(Order.Status.PENDING);
    }

    @Test
    @DisplayName("new Order — totalAmount defaults to ZERO")
    void newOrder_totalAmount_defaultsZero() {
        assertThat(new Order().getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("new Order — items list is initialised empty, not null")
    void newOrder_itemsList_initialisedEmpty() {
        assertThat(new Order().getItems()).isNotNull().isEmpty();
    }

    // ─── Status enum ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Status enum — contains all 5 expected values")
    void statusEnum_containsAllValues() {
        assertThat(Order.Status.values()).containsExactlyInAnyOrder(
                Order.Status.PENDING,
                Order.Status.CONFIRMED,
                Order.Status.SHIPPED,
                Order.Status.DELIVERED,
                Order.Status.CANCELLED
        );
    }

    @Test
    @DisplayName("setStatus — updates status correctly through all transitions")
    void setStatus_updatesCorrectly() {
        order.setStatus(Order.Status.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);

        order.setStatus(Order.Status.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(Order.Status.SHIPPED);

        order.setStatus(Order.Status.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(Order.Status.DELIVERED);
    }

    // ─── PaymentMethod enum ───────────────────────────────────────────────────

    @Test
    @DisplayName("PaymentMethod enum — contains all 6 expected values")
    void paymentMethodEnum_containsAllValues() {
        assertThat(Order.PaymentMethod.values()).containsExactlyInAnyOrder(
                Order.PaymentMethod.CREDIT_CARD,
                Order.PaymentMethod.DEBIT_CARD,
                Order.PaymentMethod.PAYPAL,
                Order.PaymentMethod.BKASH,
                Order.PaymentMethod.NAGAD,
                Order.PaymentMethod.CASH_ON_DELIVERY
        );
    }

    @Test
    @DisplayName("setPaymentMethod — stores and retrieves payment method")
    void setPaymentMethod_roundTrips() {
        order.setPaymentMethod(Order.PaymentMethod.BKASH);
        assertThat(order.getPaymentMethod()).isEqualTo(Order.PaymentMethod.BKASH);
    }

    // ─── fields ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setTotalAmount — stores BigDecimal with full precision")
    void setTotalAmount_storesPrecisely() {
        order.setTotalAmount(new BigDecimal("149.99"));
        assertThat(order.getTotalAmount()).isEqualByComparingTo("149.99");
    }

    @Test
    @DisplayName("setPaymentReference — round-trips correctly")
    void setPaymentReference_roundTrips() {
        order.setPaymentReference("TXN-20260228-ABC123");
        assertThat(order.getPaymentReference()).isEqualTo("TXN-20260228-ABC123");
    }

    @Test
    @DisplayName("setBuyer — holds the correct buyer reference")
    void setBuyer_holdsReference() {
        assertThat(order.getBuyer()).isSameAs(buyer);
        assertThat(order.getBuyer().getUsername()).isEqualTo("buyer1");
    }

    @Test
    @DisplayName("getItems — items added to list are retrievable")
    void getItems_returnsAddedItems() {
        OrderItem item = new OrderItem();
        item.setQuantity(2);
        order.getItems().add(item);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
    }
}
