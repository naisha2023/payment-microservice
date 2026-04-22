package org.example.ledgerservice.repository;

import org.example.ledgerservice.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByLedgerAccountIdOrderByCreatedAtDesc(UUID ledgerAccountId);

    List<LedgerEntry> findByLedgerTransactionId(UUID ledgerTransactionId);

    List<LedgerEntry> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    Optional<LedgerEntry> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}