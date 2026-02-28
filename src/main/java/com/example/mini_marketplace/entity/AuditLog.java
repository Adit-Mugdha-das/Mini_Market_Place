package com.example.mini_marketplace.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor",  columnList = "actor_username"),
        @Index(name = "idx_audit_action", columnList = "action_type"),
        @Index(name = "idx_audit_ts",     columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    // ── Action types ────────────────────────────────────────────────────────
    public enum ActionType {
        // Product actions
        CREATE_PRODUCT,
        UPDATE_PRODUCT,
        DELETE_PRODUCT,

        // Order actions
        PLACE_ORDER,
        CANCEL_ORDER,
        ADVANCE_ORDER_STATUS,
        OVERRIDE_ORDER_STATUS,

        // User / admin actions
        REGISTER_USER,
        DELETE_USER,
        TOGGLE_USER_STATUS,
        CHANGE_USER_ROLE,

        // Admin product action
        ADMIN_DELETE_PRODUCT
    }

    // ── Entity types ────────────────────────────────────────────────────────
    public enum EntityType {
        PRODUCT, ORDER, USER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The username of whoever performed the action. */
    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    /** The ID of the entity that was acted upon (product id, order id, user id). */
    @Column(name = "entity_id")
    private Long entityId;

    /** Optional human-readable detail, e.g. "Status changed to SHIPPED". */
    @Column(length = 500)
    private String details;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
