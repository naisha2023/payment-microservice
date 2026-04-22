package org.example.ledgerservice.service;

import org.example.ledgerservice.entity.LedgerTransaction;
import org.example.shared.enums.AccountType;
import org.example.shared.interfaces.LedgerEvent;

import java.math.BigDecimal;
import java.util.UUID;
import org.example.ledgerservice.entity.LedgerAccount;
import org.example.ledgerservice.dto.CreateLedgerTransactionRequest;
import org.example.ledgerservice.dto.PaymentCreatedEvent;

public interface LedgerTransactionService {
    LedgerTransaction createTransaction(LedgerTransaction ledgerTransaction);
    LedgerAccount createAccount(UUID userId, AccountType accountType, String currency, BigDecimal initialBalance);
    LedgerTransaction createTransaction(CreateLedgerTransactionRequest request);
    void handlePaymentCreated(PaymentCreatedEvent event);
    void createEntry(LedgerEvent event);
}
