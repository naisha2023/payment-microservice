package org.example.ledgerservice.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.example.ledgerservice.dto.CreateLedgerTransactionRequest;
import org.example.ledgerservice.entity.LedgerAccount;
import org.example.ledgerservice.entity.LedgerTransaction;
import org.example.ledgerservice.service.LedgerTransactionServiceImpl;
import org.example.shared.enums.AccountType;
import org.example.shared.interfaces.Auditable;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerTransactionServiceImpl ledgerService;

    @PostMapping("/accounts")
    @Auditable(action = "CREATE ACCOUNT", resource = "LEDGER")
    public LedgerAccount createAccount(
            @RequestParam UUID userId,
            @RequestParam AccountType accountType,
            @RequestParam String currency
    ) {
        return ledgerService.createAccount(userId, accountType, currency, BigDecimal.ZERO);
    }

    @PostMapping("/transactions")
    @Auditable(action = "CREATE TRANSACTION", resource = "LEDGER")
    public LedgerTransaction createTransaction(@RequestBody CreateLedgerTransactionRequest request) {
        return ledgerService.createTransaction(request);
    }
}