package com.example.mini_marketplace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

// Unit tests for AuditLog entity model
@DisplayName("AuditLog Entity Unit Tests")
class AuditLogEntityTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    // ─── no-arg constructor / defaults ────────────────────────────────────────

    @Test
    @DisplayName("new AuditLog — object is not null after no-arg construction")
    void noArgConstructor_producesNonNull() {
        assertThat(new AuditLog()).isNotNull();
    }

    @Test
    @DisplayName("new AuditLog — all fields are null by default")
    void newAuditLog_allFieldsNullByDefault() {
        AuditLog fresh = new AuditLog();
        assertThat(fresh.getId()).isNull();
        assertThat(fresh.getActorUsername()).isNull();
        assertThat(fresh.getActionType()).isNull();
        assertThat(fresh.getEntityType()).isNull();
        assertThat(fresh.getEntityId()).isNull();
        assertThat(fresh.getDetails()).isNull();
        assertThat(fresh.getTimestamp()).isNull();
    }

    // ─── setters / getters ────────────────────────────────────────────────────

    @Test
    @DisplayName("setActorUsername / getActorUsername — round-trips correctly")
    void actorUsername_roundTrips() {
        auditLog.setActorUsername("admin");
        assertThat(auditLog.getActorUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("setActionType / getActionType — stores CREATE_PRODUCT correctly")
    void actionType_storesCreateProduct() {
        auditLog.setActionType(AuditLog.ActionType.CREATE_PRODUCT);
        assertThat(auditLog.getActionType()).isEqualTo(AuditLog.ActionType.CREATE_PRODUCT);
    }

    @Test
    @DisplayName("setEntityType / getEntityType — stores each EntityType correctly")
    void entityType_storesAllValues() {
        for (AuditLog.EntityType type : AuditLog.EntityType.values()) {
            auditLog.setEntityType(type);
            assertThat(auditLog.getEntityType()).isEqualTo(type);
        }
    }

    @Test
    @DisplayName("setEntityId / getEntityId — round-trips correctly")
    void entityId_roundTrips() {
        auditLog.setEntityId(42L);
        assertThat(auditLog.getEntityId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("setDetails / getDetails — stores detail string correctly")
    void details_roundTrips() {
        auditLog.setDetails("Status changed to SHIPPED");
        assertThat(auditLog.getDetails()).isEqualTo("Status changed to SHIPPED");
    }

    // ─── ActionType enum ──────────────────────────────────────────────────────

    @Test
    @DisplayName("ActionType enum — contains all expected product action values")
    void actionTypeEnum_containsProductActions() {
        assertThat(AuditLog.ActionType.values())
                .contains(
                        AuditLog.ActionType.CREATE_PRODUCT,
                        AuditLog.ActionType.UPDATE_PRODUCT,
                        AuditLog.ActionType.DELETE_PRODUCT,
                        AuditLog.ActionType.ADMIN_DELETE_PRODUCT);
    }

    @Test
    @DisplayName("ActionType enum — contains all expected order action values")
    void actionTypeEnum_containsOrderActions() {
        assertThat(AuditLog.ActionType.values())
                .contains(
                        AuditLog.ActionType.PLACE_ORDER,
                        AuditLog.ActionType.CANCEL_ORDER,
                        AuditLog.ActionType.ADVANCE_ORDER_STATUS,
                        AuditLog.ActionType.OVERRIDE_ORDER_STATUS);
    }

    @Test
    @DisplayName("ActionType enum — contains all expected user/admin action values")
    void actionTypeEnum_containsUserActions() {
        assertThat(AuditLog.ActionType.values())
                .contains(
                        AuditLog.ActionType.REGISTER_USER,
                        AuditLog.ActionType.DELETE_USER,
                        AuditLog.ActionType.TOGGLE_USER_STATUS,
                        AuditLog.ActionType.CHANGE_USER_ROLE);
    }

    // ─── EntityType enum ──────────────────────────────────────────────────────

    @Test
    @DisplayName("EntityType enum — contains exactly PRODUCT, ORDER and USER")
    void entityTypeEnum_containsExactlyThreeValues() {
        assertThat(AuditLog.EntityType.values())
                .containsExactlyInAnyOrder(
                        AuditLog.EntityType.PRODUCT,
                        AuditLog.EntityType.ORDER,
                        AuditLog.EntityType.USER);
    }

    // ─── full population ──────────────────────────────────────────────────────

    @Test
    @DisplayName("fully populated AuditLog — all fields retrievable after setting")
    void fullyPopulated_allFieldsRetrievable() {
        auditLog.setId(1L);
        auditLog.setActorUsername("seller1");
        auditLog.setActionType(AuditLog.ActionType.PLACE_ORDER);
        auditLog.setEntityType(AuditLog.EntityType.ORDER);
        auditLog.setEntityId(99L);
        auditLog.setDetails("Bought 3x Widget via BKASH");

        assertThat(auditLog.getId()).isEqualTo(1L);
        assertThat(auditLog.getActorUsername()).isEqualTo("seller1");
        assertThat(auditLog.getActionType()).isEqualTo(AuditLog.ActionType.PLACE_ORDER);
        assertThat(auditLog.getEntityType()).isEqualTo(AuditLog.EntityType.ORDER);
        assertThat(auditLog.getEntityId()).isEqualTo(99L);
        assertThat(auditLog.getDetails()).contains("Widget");
    }
}
