package org.example.ledgerservice.messaging;

import java.math.BigDecimal;

import org.example.ledgerservice.entity.LedgerAccount;
import org.example.ledgerservice.repository.LedgerAccountRepository;
import org.example.ledgerservice.service.LedgerTransactionService;
import org.example.shared.config.KafkaTopics;
import org.example.shared.enums.AccountType;
import org.example.shared.event.WalletCreatedEvent;
import org.example.shared.event.WalletFundedEvent;
import org.example.shared.event.WalletReleaseFundedEvent;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletcOperationConsumer {

    private static final String GROUP_ID = "ledger-service";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerTransactionService ledgerTransactionService;

    @KafkaListener(
            topics = KafkaTopics.WALLET_CREATED,
            groupId = GROUP_ID
    )
    @Transactional
    public void handle(
            WalletCreatedEvent event,
            @Header(name = "X-Correlation-ID", required = false) String correlationId
    ) {
        withCorrelationId(correlationId, () -> handleWalletCreated(event, correlationId));
    }

    @KafkaListener(
            topics = KafkaTopics.WALLET_FUNDED,
            groupId = GROUP_ID
    )
    @Transactional
    public void handle(
            WalletFundedEvent event,
            @Header(name = "X-Correlation-ID", required = false) String correlationId
    ) {
        withCorrelationId(correlationId, () -> handleWalletFunded(event, correlationId));
    }

    @KafkaListener(
            topics = KafkaTopics.WALLET_RELEASE_FUNDED,
            groupId = GROUP_ID
    )
    @Transactional
    public void handle(
            WalletReleaseFundedEvent event,
            @Header(name = "X-Correlation-ID", required = false) String correlationId
    ) {
        withCorrelationId(correlationId, () -> handleWalletReleaseFunded(event, correlationId));
    }

    private void handleWalletCreated(WalletCreatedEvent event, String correlationId) {
        log.info(
                "wallet_created_event_received walletId={} currency={} correlationId={}",
                event.walletId(),
                event.currency(),
                correlationId
        );

        boolean exists = ledgerAccountRepository.findByWalletId(event.walletId()).isPresent();

        if (exists) {
            log.info(
                    "ledger_account_already_exists walletId={} correlationId={}",
                    event.walletId(),
                    correlationId
            );
            return;
        }

        ledgerTransactionService.createAccount(
                event.walletId(),
                event.accountType() != null ? event.accountType() : AccountType.CUSTOMER,
                event.currency(),
                event.initialBalance() != null ? event.initialBalance() : BigDecimal.ZERO
        );

        log.info(
                "ledger_account_created walletId={} currency={} correlationId={}",
                event.walletId(),
                event.currency(),
                correlationId
        );
    }

    private void handleWalletFunded(WalletFundedEvent event, String correlationId) {
        log.info(
                "wallet_funded_event_received walletId={} amount={} correlationId={}",
                event.walletId(),
                event.amount(),
                correlationId
        );

        LedgerAccount account = ledgerAccountRepository.findByWalletId(event.walletId())
                .orElseThrow(() -> {
                    log.error(
                            "ledger_account_not_found walletId={} event=wallet_funded correlationId={}",
                            event.walletId(),
                            correlationId
                    );
                    return new IllegalStateException("LedgerAccount no existe");
                });

        BigDecimal before = account.getAvailableBalance();
        BigDecimal after = before.add(event.amount());

        account.setAvailableBalance(after);
        ledgerAccountRepository.save(account);

        ledgerTransactionService.createEntry(event);

        log.info(
                "ledger_wallet_funded_recorded walletId={} amount={} availableBefore={} availableAfter={} correlationId={}",
                event.walletId(),
                event.amount(),
                before,
                after,
                correlationId
        );
    }

    private void handleWalletReleaseFunded(WalletReleaseFundedEvent event, String correlationId) {
        log.info(
                "wallet_release_funded_event_received walletId={} amount={} correlationId={}",
                event.walletId(),
                event.amount(),
                correlationId
        );

        LedgerAccount account = ledgerAccountRepository.findByWalletId(event.walletId())
                .orElseThrow(() -> {
                    log.error(
                            "ledger_account_not_found walletId={} event=wallet_release_funded correlationId={}",
                            event.walletId(),
                            correlationId
                    );
                    return new IllegalStateException("LedgerAccount no existe");
                });

        BigDecimal availableBefore = account.getAvailableBalance();
        BigDecimal reservedBefore = account.getReservedBalance();

        BigDecimal availableAfter = availableBefore.add(event.amount());
        BigDecimal reservedAfter = reservedBefore.subtract(event.amount());

        account.setAvailableBalance(availableAfter);
        account.setReservedBalance(reservedAfter);
        ledgerAccountRepository.save(account);

        ledgerTransactionService.createEntry(event);

        log.info(
                "ledger_wallet_release_recorded walletId={} amount={} availableBefore={} availableAfter={} reservedBefore={} reservedAfter={} correlationId={}",
                event.walletId(),
                event.amount(),
                availableBefore,
                availableAfter,
                reservedBefore,
                reservedAfter,
                correlationId
        );
    }

    private void withCorrelationId(String correlationId, Runnable action) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlation_id", correlationId);
        }

        try {
            action.run();
        } finally {
            MDC.remove("correlation_id");
        }
    }
}