package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

// Unit tests for Category entity model
@DisplayName("Category Entity Unit Tests")
class CategoryEntityTest {

    // ─── no-arg constructor ───────────────────────────────────────────────────

    @Test
    @DisplayName("no-arg constructor — produces non-null object with null fields")
    void noArgConstructor_producesNonNull_withNullFields() {
        Category cat = new Category();
        assertThat(cat).isNotNull();
        assertThat(cat.getId()).isNull();
        assertThat(cat.getName()).isNull();
        assertThat(cat.getDescription()).isNull();
    }

    // ─── all-arg constructor ──────────────────────────────────────────────────

    @Test
    @DisplayName("all-arg constructor — sets name and description correctly")
    void allArgConstructor_setsNameAndDescription() {
        Category cat = new Category("Electronics", "All electronic items");
        assertThat(cat.getName()).isEqualTo("Electronics");
        assertThat(cat.getDescription()).isEqualTo("All electronic items");
    }

    @Test
    @DisplayName("all-arg constructor — allows null description")
    void allArgConstructor_allowsNullDescription() {
        Category cat = new Category("Books", null);
        assertThat(cat.getName()).isEqualTo("Books");
        assertThat(cat.getDescription()).isNull();
    }

    // ─── setters / getters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setName / getName — round-trips correctly")
    void name_roundTrips() {
        Category cat = new Category();
        cat.setName("Clothing");
        assertThat(cat.getName()).isEqualTo("Clothing");
    }

    @Test
    @DisplayName("setDescription / getDescription — round-trips correctly")
    void description_roundTrips() {
        Category cat = new Category();
        cat.setDescription("All types of clothing and apparel");
        assertThat(cat.getDescription()).isEqualTo("All types of clothing and apparel");
    }

    @Test
    @DisplayName("setId / getId — round-trips correctly")
    void id_roundTrips() {
        Category cat = new Category();
        cat.setId(7L);
        assertThat(cat.getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("setName — overwrites previously set name")
    void setName_overwritesPreviousValue() {
        Category cat = new Category("OldName", "desc");
        cat.setName("NewName");
        assertThat(cat.getName()).isEqualTo("NewName");
    }

    @Test
    @DisplayName("two distinct Category objects — are independent of each other")
    void twoCategories_areIndependent() {
        Category cat1 = new Category("Sports", "Sports gear");
        Category cat2 = new Category("Toys", "Children toys");
        cat1.setId(1L);
        cat2.setId(2L);

        assertThat(cat1.getName()).isNotEqualTo(cat2.getName());
        assertThat(cat1.getId()).isNotEqualTo(cat2.getId());
    }
}
