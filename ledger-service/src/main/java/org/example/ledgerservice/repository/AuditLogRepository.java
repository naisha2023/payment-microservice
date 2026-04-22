package org.example.ledgerservice.repository;

import java.util.UUID;

import org.example.ledgerservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}