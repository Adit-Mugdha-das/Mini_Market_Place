package com.example.mini_marketplace.service;

import com.example.mini_marketplace.entity.AuditLog;
import com.example.mini_marketplace.entity.AuditLog.ActionType;
import com.example.mini_marketplace.entity.AuditLog.EntityType;
import com.example.mini_marketplace.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Unit tests for Audit logging mechanism
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    // ─── log (5-arg) ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("log — saves AuditLog with all correct fields")
    void log_savesAuditLogWithAllFields() {
        auditService.log("admin", ActionType.CREATE_PRODUCT, EntityType.PRODUCT, 42L, "Created: Laptop");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActorUsername()).isEqualTo("admin");
        assertThat(saved.getActionType()).isEqualTo(ActionType.CREATE_PRODUCT);
        assertThat(saved.getEntityType()).isEqualTo(EntityType.PRODUCT);
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getDetails()).isEqualTo("Created: Laptop");
    }

    @Test
    @DisplayName("log — saves AuditLog for DELETE_USER action")
    void log_savesAuditLog_forDeleteUser() {
        auditService.log("admin", ActionType.DELETE_USER, EntityType.USER, 7L, "Deleted user: buyer1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActorUsername()).isEqualTo("admin");
        assertThat(saved.getActionType()).isEqualTo(ActionType.DELETE_USER);
        assertThat(saved.getEntityType()).isEqualTo(EntityType.USER);
        assertThat(saved.getEntityId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("log — saves AuditLog for PLACE_ORDER action")
    void log_savesAuditLog_forPlaceOrder() {
        auditService.log("buyer1", ActionType.PLACE_ORDER, EntityType.ORDER, 99L, "Bought 2x Widget");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActorUsername()).isEqualTo("buyer1");
        assertThat(saved.getActionType()).isEqualTo(ActionType.PLACE_ORDER);
        assertThat(saved.getEntityId()).isEqualTo(99L);
        assertThat(saved.getDetails()).contains("Widget");
    }

    @Test
    @DisplayName("log — saves AuditLog for ADVANCE_ORDER_STATUS action")
    void log_savesAuditLog_forAdvanceOrderStatus() {
        auditService.log("seller1", ActionType.ADVANCE_ORDER_STATUS, EntityType.ORDER,
                55L, "Status changed to SHIPPED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActionType()).isEqualTo(ActionType.ADVANCE_ORDER_STATUS);
        assertThat(saved.getDetails()).isEqualTo("Status changed to SHIPPED");
    }

    @Test
    @DisplayName("log — does not propagate exception when repository throws")
    void log_doesNotThrow_whenRepositoryFails() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        // Should silently swallow the error — never crash the main flow
        assertThatNoException().isThrownBy(() ->
                auditService.log("admin", ActionType.DELETE_USER, EntityType.USER, 1L, "test"));
    }

    @Test
    @DisplayName("log (4-arg overload) — saves AuditLog with null details")
    void log_4arg_savesWithNullDetails() {
        auditService.log("admin", ActionType.TOGGLE_USER_STATUS, EntityType.USER, 3L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        // called twice because overload delegates to 5-arg
        verify(auditLogRepository, atLeastOnce()).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActorUsername()).isEqualTo("admin");
        assertThat(saved.getEntityId()).isEqualTo(3L);
        assertThat(saved.getDetails()).isNull();
    }

    // ─── getRecentLogs ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRecentLogs — delegates to repository with correct page params")
    void getRecentLogs_delegatesCorrectly() {
        AuditLog log1 = new AuditLog();
        log1.setActorUsername("admin");
        log1.setActionType(ActionType.CREATE_PRODUCT);

        Page<AuditLog> fakePage = new PageImpl<>(List.of(log1),
                PageRequest.of(0, 20), 1);
        when(auditLogRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(fakePage);

        Page<AuditLog> result = auditService.getRecentLogs(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActorUsername()).isEqualTo("admin");
        verify(auditLogRepository).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("getRecentLogs — returns empty page when no logs exist")
    void getRecentLogs_returnsEmptyPage_whenNoLogs() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of(),
                PageRequest.of(0, 20), 0);
        when(auditLogRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> result = auditService.getRecentLogs(0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
