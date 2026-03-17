package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

// Unit tests for User entity fields and behavior
@DisplayName("User Entity Unit Tests")
class UserEntityTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Jane Doe");
        user.setUsername("janedoe");
        user.setEmail("jane@example.com");
        user.setPassword("$2a$ENCODED");
    }

    // ─── defaults ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("new User — enabled defaults to true")
    void newUser_enabledDefaultsToTrue() {
        assertThat(new User().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("new User — roles set is initialised empty, not null")
    void newUser_rolesSet_initialisedEmpty() {
        assertThat(new User().getRoles()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("no-arg constructor — produces a non-null User object")
    void noArgConstructor_producesNonNull() {
        assertThat(new User()).isNotNull();
    }

    // ─── getters / setters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setFullName / getFullName — round-trips correctly")
    void fullName_roundTrips() {
        user.setFullName("John Smith");
        assertThat(user.getFullName()).isEqualTo("John Smith");
    }

    @Test
    @DisplayName("setUsername / getUsername — round-trips correctly")
    void username_roundTrips() {
        user.setUsername("johnsmith");
        assertThat(user.getUsername()).isEqualTo("johnsmith");
    }

    @Test
    @DisplayName("setEmail / getEmail — round-trips correctly")
    void email_roundTrips() {
        user.setEmail("john@test.com");
        assertThat(user.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    @DisplayName("setPassword / getPassword — stores encoded password")
    void password_roundTrips() {
        user.setPassword("$2a$10$NEWHASH");
        assertThat(user.getPassword()).isEqualTo("$2a$10$NEWHASH");
    }

    // ─── enabled toggle ───────────────────────────────────────────────────────

    @Test
    @DisplayName("setEnabled(false) — marks user as disabled")
    void setEnabled_false_disablesUser() {
        user.setEnabled(false);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("setEnabled(true) — re-enables a disabled user")
    void setEnabled_true_reEnablesUser() {
        user.setEnabled(false);
        user.setEnabled(true);
        assertThat(user.isEnabled()).isTrue();
    }

    // ─── roles ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setRoles — stores and retrieves assigned roles")
    void setRoles_storesCorrectly() {
        Role buyerRole  = new Role(Role.RoleName.ROLE_BUYER);
        Role sellerRole = new Role(Role.RoleName.ROLE_SELLER);

        user.setRoles(Set.of(buyerRole, sellerRole));

        assertThat(user.getRoles()).hasSize(2);
        assertThat(user.getRoles())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(
                        Role.RoleName.ROLE_BUYER,
                        Role.RoleName.ROLE_SELLER);
    }

    @Test
    @DisplayName("setRoles — replacing roles overwrites previous set")
    void setRoles_replacesExistingRoles() {
        user.setRoles(Set.of(new Role(Role.RoleName.ROLE_BUYER)));
        user.setRoles(Set.of(new Role(Role.RoleName.ROLE_SELLER)));

        assertThat(user.getRoles())
                .extracting(Role::getName)
                .containsExactly(Role.RoleName.ROLE_SELLER);
    }

    @Test
    @DisplayName("getId — returns the id that was set")
    void getId_returnsSetId() {
        user.setId(99L);
        assertThat(user.getId()).isEqualTo(99L);
    }
}
