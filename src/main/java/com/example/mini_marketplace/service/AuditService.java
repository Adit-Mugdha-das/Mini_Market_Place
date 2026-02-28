package com.example.mini_marketplace.service;

import com.example.mini_marketplace.entity.AuditLog;
import com.example.mini_marketplace.entity.AuditLog.ActionType;
import com.example.mini_marketplace.entity.AuditLog.EntityType;
import com.example.mini_marketplace.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Persist an audit entry asynchronously — never blocks the main transaction.
     * Runs in a NEW transaction so caller rollback does not lose the log.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, ActionType actionType, EntityType entityType,
                    Long entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActorUsername(actor);
            entry.setActionType(actionType);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details);
            auditLogRepository.save(entry);
            log.info("[AUDIT] {} | {} | {}#{} | {}", actor, actionType, entityType, entityId, details);
        } catch (Exception e) {
            log.error("[AUDIT] Failed to persist audit log: {}", e.getMessage());
        }
    }

    /** Convenience overload — no detail string. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, ActionType actionType, EntityType entityType, Long entityId) {
        log(actor, actionType, entityType, entityId, null);
    }

    public Page<AuditLog> getRecentLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(
                PageRequest.of(page, size, Sort.by("timestamp").descending()));
    }
}
