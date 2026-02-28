package com.example.mini_marketplace.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InsufficientStockException Unit Tests")
class InsufficientStockExceptionTest {

    @Test
    @DisplayName("getMessage — contains product name, requested and available quantity")
    void message_containsAllFields() {
        InsufficientStockException ex =
                new InsufficientStockException("Wireless Mouse", 5, 2);

        assertThat(ex.getMessage())
                .contains("Wireless Mouse")
                .contains("5")
                .contains("2");
    }

    @Test
    @DisplayName("getters — return the exact values passed to constructor")
    void getters_returnCorrectValues() {
        InsufficientStockException ex =
                new InsufficientStockException("Laptop", 10, 3);

        assertThat(ex.getProductName()).isEqualTo("Laptop");
        assertThat(ex.getRequested()).isEqualTo(10);
        assertThat(ex.getAvailable()).isEqualTo(3);
    }

    @Test
    @DisplayName("isInstanceOf — is a RuntimeException so it is unchecked")
    void isUnchecked_runtimeException() {
        InsufficientStockException ex =
                new InsufficientStockException("Keyboard", 1, 0);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("available zero — message still formats correctly for out-of-stock case")
    void message_formatsCorrectly_whenAvailableIsZero() {
        InsufficientStockException ex =
                new InsufficientStockException("Headphones", 3, 0);

        assertThat(ex.getMessage())
                .contains("Headphones")
                .contains("Requested: 3")
                .contains("Available: 0");
    }

    @Test
    @DisplayName("getMessage — superclass getMessage() equals the formatted string")
    void message_equalsRuntimeExceptionGetMessage() {
        String productName = "USB Hub";
        int requested = 7, available = 1;

        InsufficientStockException ex =
                new InsufficientStockException(productName, requested, available);

        // The message passed to super() must be exactly what getMessage() returns
        String expected = String.format(
                "Not enough stock for \"%s\". Requested: %d, Available: %d.",
                productName, requested, available);

        assertThat(ex.getMessage()).isEqualTo(expected);
    }

    @Test
    @DisplayName("constructor — handles product name with special characters correctly")
    void constructor_handlesSpecialCharactersInProductName() {
        InsufficientStockException ex =
                new InsufficientStockException("Monitor 27\" 4K & HDR (2026)", 2, 0);

        assertThat(ex.getProductName()).isEqualTo("Monitor 27\" 4K & HDR (2026)");
        assertThat(ex.getMessage()).contains("Monitor 27\" 4K & HDR (2026)");
    }

    @Test
    @DisplayName("constructor — requested larger than available always reflects both values correctly")
    void constructor_requestedAlwaysGreaterThanAvailable_reflectedCorrectly() {
        int requested = Integer.MAX_VALUE;
        int available = Integer.MAX_VALUE - 1;

        InsufficientStockException ex =
                new InsufficientStockException("Mega Item", requested, available);

        assertThat(ex.getRequested()).isEqualTo(requested);
        assertThat(ex.getAvailable()).isEqualTo(available);
        assertThat(ex.getMessage())
                .contains(String.valueOf(requested))
                .contains(String.valueOf(available));
    }
}
