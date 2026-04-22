package org.example.shared.event;

import java.math.BigDecimal;
import java.util.UUID;

import org.example.shared.enums.AccountType;

public record WalletCreatedEvent(UUID walletId, AccountType accountType, String currency, BigDecimal initialBalance, UUID userId) {
    
}
