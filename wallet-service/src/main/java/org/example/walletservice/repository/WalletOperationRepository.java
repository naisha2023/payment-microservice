package org.example.walletservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.example.walletservice.entity.WalletOperation;
import org.example.walletservice.enums.WalletOperationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletOperationRepository extends JpaRepository<WalletOperation, UUID> {

    boolean existsByWalletIdAndPaymentIdAndOperationType(
            UUID walletId,
            UUID paymentId,
            WalletOperationType operationType
    );

    Optional<WalletOperation> findByWalletIdAndPaymentIdAndOperationType(
            UUID walletId,
            UUID paymentId,
            WalletOperationType operationType
    );
    
}
