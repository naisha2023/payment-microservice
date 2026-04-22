package org.example.ledgerservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.example.ledgerservice.entity.LedgerTransaction;
import org.example.ledgerservice.entity.OutboxEvent;
import org.example.ledgerservice.repository.LedgerTransactionRepository;
import org.example.ledgerservice.repository.OutboxEventRepository;
import org.example.shared.enums.AccountType;
import org.example.shared.interfaces.LedgerEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.ledgerservice.repository.LedgerAccountRepository;
import org.example.ledgerservice.repository.LedgerRepository;
import org.example.ledgerservice.repository.AuditLogRepository; 
import java.util.Map;

import org.example.ledgerservice.entity.LedgerAccount;
import java.math.BigDecimal;

import org.example.ledgerservice.enums.TransactionType;

import java.time.LocalDateTime;
import org.example.ledgerservice.entity.LedgerEntry;
import org.example.ledgerservice.constants.LedgerConstants;
import org.example.ledgerservice.dto.CreateLedgerTransactionRequest;
import org.example.ledgerservice.dto.PaymentCreatedEvent;
import org.example.ledgerservice.enums.TransactionStatus;
import org.example.ledgerservice.entity.AuditLog;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerTransactionServiceImpl implements LedgerTransactionService {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LedgerTransaction createTransaction(LedgerTransaction ledgerTransaction) {

        if (ledgerTransactionRepository.existsByReferenceId(ledgerTransaction.getReferenceId())) {
            throw new IllegalArgumentException("Transaction already exists with referenceId");
        }

        LedgerTransaction savedTx = ledgerTransactionRepository.save(ledgerTransaction);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("ledger.transaction");
        event.setAggregateId(savedTx.getId());
        event.setEventType("ledger.transaction.created");
        event.setPublished(false);

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "transactionId", savedTx.getId(),
                    "referenceId", savedTx.getReferenceId(),
                    "amount", savedTx.getAmount(),
                    "currency", savedTx.getCurrency(),
                    "status", savedTx.getStatus()
            ));
            event.setPayload(payload);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing payload", e);
        }

        event.setLedgerTransaction(savedTx);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("LEDGER_TRANSACTION");
        auditLog.setEntityId(savedTx.getId());
        auditLog.setAction(ledgerTransaction.getDescription() != null ? ledgerTransaction.getDescription() : "TRANSACTION_CREATED");
        auditLog.setActor("SYSTEM");
        auditLog.setDetails("Ledger transaction found for referenceId=" + savedTx.getReferenceId());
        auditLog.setLedgerTransaction(savedTx);

        auditLogRepository.save(auditLog);  

        outboxEventRepository.save(event);
        System.out.println("Ledger transaction found with ID: " + savedTx.getId());
        return savedTx;
    }

    public void createEntry(PaymentCreatedEvent event) {
        if (ledgerTransactionRepository.existsByReferenceId(event.paymentId().toString())) {
            return;
        }

        LedgerTransaction tx = mapToLedgerTransaction(event);
        createTransaction(tx);
    }

    public void createEntry(LedgerEvent event) {
        if (ledgerTransactionRepository.existsByReferenceId(event.getReferenceId().toString())) {
            return;
        }

        LedgerTransaction tx = mapToLedgerTransaction(event);
        createTransaction(tx);
    }

    private LedgerTransaction mapToLedgerTransaction(LedgerEvent event) {
        LedgerTransaction tx = new LedgerTransaction();
        tx.setPaymentId(UUID.fromString(event.getReferenceId()));
        tx.setReferenceId(event.getReferenceId().toString());
        tx.setTransactionType(TransactionType.CONFIRM_DEBIT);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setAmount(event.getAmount());
        tx.setCurrency(event.getCurrency());
        tx.setReference(event.getReferenceId().toString());
        tx.setDescription(event.reason());
        tx.setPostedAt(LocalDateTime.now());
        return tx;
    }

    private LedgerTransaction mapToLedgerTransaction(PaymentCreatedEvent event) {
        LedgerTransaction tx = new LedgerTransaction();
        tx.setPaymentId(event.paymentId());
        tx.setReferenceId(event.paymentId().toString());
        tx.setTransactionType(TransactionType.CONFIRM_DEBIT);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setAmount(event.amount());
        tx.setCurrency(event.currency());
        tx.setReference(event.paymentId().toString());
        tx.setDescription(event.description());
        tx.setPostedAt(LocalDateTime.now());
        return tx;
    }

    @Override
    @Transactional
    public LedgerAccount createAccount(UUID walletId, AccountType accountType, String currency, BigDecimal initialBalance) {
        LedgerAccount account = new LedgerAccount();
        account.setWalletId(walletId);
        account.setAccountType(accountType);
        account.setCurrency(currency);
        account.setAvailableBalance(initialBalance);
        account.setReservedBalance(BigDecimal.ZERO);
        account.setActive(true);

        LedgerAccount saved = ledgerAccountRepository.save(account);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("LEDGER_ACCOUNT");
        auditLog.setEntityId(saved.getId());
        auditLog.setAction("ACCOUNT_CREATED");
        auditLog.setActor("SYSTEM");
        auditLog.setDetails("Ledger account created for walletId=" + walletId);
        auditLog.setLedgerAccount(saved);

        auditLogRepository.save(auditLog);

        return saved;
    }

    @Override
    @Transactional
    public LedgerTransaction createTransaction(CreateLedgerTransactionRequest request) {
        LedgerAccount account = ledgerAccountRepository.findByWalletId(request.accountId())
                .orElseThrow(() -> new RuntimeException("Ledger account not found"));

        if (!account.isActive()) {
            throw new RuntimeException("Ledger account is inactive");
        }

        if (!account.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new RuntimeException("Currency mismatch");
        }

        ledgerTransactionRepository.findByReferenceId(request.paymentId().toString())
                .ifPresent(tx -> {
                    throw new RuntimeException("Transaction with referenceId already exists");
                });

        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal balanceAfter = calculateNewBalance(account, request.amount(), request.transactionType());

        account.setAvailableBalance(balanceAfter);
        ledgerAccountRepository.save(account);

        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setPaymentId(request.paymentId());
        transaction.setReferenceId(request.paymentId().toString());
        transaction.setTransactionType(request.transactionType());
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setReference(request.reference());
        transaction.setDescription(request.description());
        transaction.setPostedAt(LocalDateTime.now());

        LedgerTransaction savedTransaction = ledgerTransactionRepository.save(transaction);

        LedgerEntry entry = new LedgerEntry();
        entry.setEntityType("LEDGER_TRANSACTION");
        entry.setAmount(request.amount());
        entry.setBalanceBefore(balanceBefore);
        entry.setBalanceAfter(balanceAfter);
        entry.setConcept(request.description() != null ? request.description() : request.transactionType().name());
        entry.setLedgerAccount(account);
        entry.setLedgerTransaction(savedTransaction);

        ledgerEntryRepository.save(entry);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("LEDGER_TRANSACTION");
        auditLog.setEntityId(savedTransaction.getId());
        auditLog.setAction("TRANSACTION_CREATED");
        auditLog.setActor(request.actor() != null ? request.actor() : "SYSTEM");
        auditLog.setDetails("Transaction created with referenceId=" + request.referenceId());
        auditLog.setLedgerAccount(account);
        auditLog.setLedgerTransaction(savedTransaction);

        auditLogRepository.save(auditLog);

        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("LEDGER_TRANSACTION");
            outboxEvent.setAggregateId(savedTransaction.getId());
            outboxEvent.setEventType("LEDGER_TRANSACTION_CREATED");
            outboxEvent.setPayload(objectMapper.writeValueAsString(savedTransaction));
            outboxEvent.setPublished(false);
            outboxEvent.setLedgerTransaction(savedTransaction);

            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }

        return savedTransaction;
    }

    private BigDecimal calculateNewBalance(
        LedgerAccount account,
        BigDecimal amount,
        TransactionType type
    ) {

        switch (type) {

            case CONFIRM_CREDIT:
                return account.getAvailableBalance().add(amount);

            case CONFIRM_DEBIT:
                if (account.getAvailableBalance().compareTo(amount) < 0) {
                    throw new RuntimeException("Insufficient balance");
                }
                return account.getAvailableBalance().subtract(amount);

            case REFUND:
                return account.getAvailableBalance().add(amount);

            case FEE:
                if (account.getAvailableBalance().compareTo(amount) < 0) {
                    throw new RuntimeException("Insufficient balance for fee");
                }
                return account.getAvailableBalance().subtract(amount);

            case ADJUSTMENT:
                return account.getAvailableBalance().add(amount); // puede ser + o -

            case RESERVE:
            case RELEASE:
                // ⚠️ estos NO afectan available directamente en este método
                return account.getAvailableBalance();

            default:
                throw new RuntimeException("Unsupported transaction type: " + type);
        }
    }

    @Transactional
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        if (ledgerEntryRepository.existsByEventId(event.eventId())) {
            log.info("Evento ya procesado en ledger: {}", event.eventId());
            return;
        }

        LedgerAccount ledgerAccount = ledgerAccountRepository.findByWalletId(event.walletId())
            .orElseThrow(() -> new IllegalStateException(LedgerConstants.ERROR_LEGDGER_NOT_FOUND + " " + event.walletId()));

        BigDecimal availableBefore = ledgerAccount.getAvailableBalance();
        BigDecimal reservedBefore = ledgerAccount.getReservedBalance();

        if (availableBefore.compareTo(event.amount()) < 0) {
            log.warn("Saldo contable insuficiente en ledger para wallet {}. available={}, reserve={}",
                    event.walletId(), availableBefore, event.amount());
        }

        ledgerAccount.setAvailableBalance(availableBefore.subtract(event.amount()));
        ledgerAccount.setReservedBalance(reservedBefore.add(event.amount()));
        ledgerAccount = ledgerAccountRepository.save(ledgerAccount);

        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setPaymentId(event.paymentId());
        transaction.setReferenceId(event.paymentId().toString());
        transaction.setTransactionType(TransactionType.RESERVE);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setAmount(event.amount());
        transaction.setCurrency(event.currency());
        transaction.setReference(event.paymentId().toString());
        transaction.setDescription("Reserva de fondos para payment " + event.paymentId());
        transaction.setPostedAt(LocalDateTime.now());
        transaction = ledgerTransactionRepository.save(transaction);

        LedgerEntry entry = new LedgerEntry();
        entry.setEntityType("PAYMENT");
        entry.setEventId(event.eventId());
        entry.setAmount(event.amount());
        entry.setBalanceBefore(availableBefore);
        entry.setBalanceAfter(ledgerAccount.getAvailableBalance());
        entry.setConcept("Reserva de fondos");
        entry.setLedgerAccount(ledgerAccount);
        entry.setLedgerTransaction(transaction);

        ledgerEntryRepository.save(entry);

        log.info("Reserva contable registrada para payment {}", event.paymentId());
    }
}