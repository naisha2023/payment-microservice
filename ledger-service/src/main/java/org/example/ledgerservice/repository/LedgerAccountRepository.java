package org.example.ledgerservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.example.ledgerservice.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByWalletId(UUID walletId);
}
