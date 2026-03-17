package com.example.mini_marketplace.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

// Unit tests for DTO DashboardMetrics
@DisplayName("AdminDashboardMetrics & SellerDashboardMetrics Unit Tests")
class DashboardMetricsDtoTest {

    // ─── AdminDashboardMetrics ────────────────────────────────────────────────

    @Test
    @DisplayName("AdminDashboardMetrics — all-arg constructor sets every field correctly")
    void adminMetrics_allArgConstructor_setsFields() {
        AdminDashboardMetrics m = new AdminDashboardMetrics(
                10L, 50L, 45L, 120L, new BigDecimal("9999.99"));

        assertThat(m.getTotalUsers()).isEqualTo(10L);
        assertThat(m.getTotalProducts()).isEqualTo(50L);
        assertThat(m.getActiveProducts()).isEqualTo(45L);
        assertThat(m.getTotalOrders()).isEqualTo(120L);
        assertThat(m.getTotalRevenue()).isEqualByComparingTo("9999.99");
    }

    @Test
    @DisplayName("AdminDashboardMetrics — zero values are handled without error")
    void adminMetrics_zeroValues_handledCorrectly() {
        AdminDashboardMetrics m = new AdminDashboardMetrics(
                0L, 0L, 0L, 0L, BigDecimal.ZERO);

        assertThat(m.getTotalUsers()).isZero();
        assertThat(m.getTotalProducts()).isZero();
        assertThat(m.getActiveProducts()).isZero();
        assertThat(m.getTotalOrders()).isZero();
        assertThat(m.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("AdminDashboardMetrics — null revenue does not throw on construction")
    void adminMetrics_nullRevenue_doesNotThrowOnConstruction() {
        assertThatNoException().isThrownBy(() ->
                new AdminDashboardMetrics(1L, 1L, 1L, 1L, null));
    }

    @Test
    @DisplayName("AdminDashboardMetrics — active products can be less than total products")
    void adminMetrics_activeProductsCanBeLessThanTotal() {
        AdminDashboardMetrics m = new AdminDashboardMetrics(
                5L, 20L, 12L, 30L, new BigDecimal("500.00"));

        assertThat(m.getActiveProducts()).isLessThanOrEqualTo(m.getTotalProducts());
    }

    // ─── SellerDashboardMetrics ───────────────────────────────────────────────

    @Test
    @DisplayName("SellerDashboardMetrics — all-arg constructor sets every field correctly")
    void sellerMetrics_allArgConstructor_setsFields() {
        SellerDashboardMetrics m = new SellerDashboardMetrics(
                8L, 6L, 40L, new BigDecimal("1500.00"));

        assertThat(m.getTotalProducts()).isEqualTo(8L);
        assertThat(m.getActiveProducts()).isEqualTo(6L);
        assertThat(m.getTotalOrders()).isEqualTo(40L);
        assertThat(m.getTotalRevenue()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("SellerDashboardMetrics — zero revenue is stored correctly")
    void sellerMetrics_zeroRevenue_storedCorrectly() {
        SellerDashboardMetrics m = new SellerDashboardMetrics(
                3L, 3L, 0L, BigDecimal.ZERO);

        assertThat(m.getTotalOrders()).isZero();
        assertThat(m.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("SellerDashboardMetrics — large revenue value stored with full precision")
    void sellerMetrics_largeRevenue_storedWithPrecision() {
        BigDecimal largeRevenue = new BigDecimal("999999.99");
        SellerDashboardMetrics m = new SellerDashboardMetrics(
                100L, 95L, 500L, largeRevenue);

        assertThat(m.getTotalRevenue()).isEqualByComparingTo(largeRevenue);
    }
}
