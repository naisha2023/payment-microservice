package org.example.shared.event;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDebitConfirmedEvent(
        UUID walletId,
        UUID userId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String reason
) {}