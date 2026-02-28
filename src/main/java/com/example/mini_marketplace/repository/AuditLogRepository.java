package com.example.mini_marketplace.repository;

import com.example.mini_marketplace.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // All logs, newest first (for admin full log view — paginated)
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    // Logs for a specific actor
    List<AuditLog> findByActorUsernameOrderByTimestampDesc(String actorUsername);

    // Logs for a specific action type
    List<AuditLog> findByActionTypeOrderByTimestampDesc(AuditLog.ActionType actionType);

    // Logs for a specific entity (e.g. all actions on ORDER #42)
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            AuditLog.EntityType entityType, Long entityId);
}
