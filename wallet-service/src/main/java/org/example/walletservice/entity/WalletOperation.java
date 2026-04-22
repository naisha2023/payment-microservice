package org.example.walletservice.entity;

import org.example.walletservice.enums.WalletOperationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "wallet_operations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_operation_wallet_payment_type",
                        columnNames = {"wallet_id", "payment_id", "operation_type"}
                )
        }
)

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletOperation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private WalletOperationType operationType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}