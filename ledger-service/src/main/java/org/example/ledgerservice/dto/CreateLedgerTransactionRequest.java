package org.example.ledgerservice.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.example.ledgerservice.enums.TransactionType;

public record CreateLedgerTransactionRequest(
    UUID paymentId,
    String referenceId,
    TransactionType transactionType,
    BigDecimal amount,
    String currency,
    String reference,
    String description,
    UUID accountId,
    String actor
) {

}
