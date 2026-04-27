package org.example.shared.repository;

import java.util.List;
import org.example.shared.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByResourceAndAction(String resource, String action);
}