package org.example.notificationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerTransactionCreatedEvent(
        UUID transactionId,
        String referenceId,
        BigDecimal amount,
        String currency,
        String status
) {
}